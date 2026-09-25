<?php
/*
 * Party relay for the RT4 client's PartyPanel plugin.
 *
 * View-only by design: members POST snapshots of their own state and read
 * back everyone else's. The server never interprets the state beyond
 * validating its shape, and no field can make another client do anything.
 *
 * One endpoint, one request per poll:
 *   POST {"room":<64 hex>, "member":<32 hex>, "name":str, "since":int,
 *         "patch":{...}|null, "leave":bool}
 *   -> {"ok":true, "seq":int, "present":[member ids], "members":[changed members]}
 *
 * "room" is SHA-256 of the party password, computed client side, so the
 * password itself never reaches this server.
 */

declare(strict_types=1);

const MAX_BODY = 16384;          // bytes per request
const MAX_MEMBERS = 20;          // per room
const STALE_SECONDS = 300;       // members silent this long are removed
const DATA_DIR = __DIR__ . '/../party-data';

// Shape of every accepted state key: [max rows, columns per row] for int
// matrices, or 'int' for a single integer. Anything else is rejected.
const SCHEMA = [
    'inv'     => [28, 2],   // [objId, qty] per slot, -1 id = empty
    'gear'    => [14, 2],   // [objId, qty] per equipment slot
    'skills'  => [25, 3],   // [base level, boosted level, xp]
    'prayers' => 'int',     // active prayer bitmask
    'prayer'  => [1, 2],    // [current points, max points]
    'world'   => 'int',
    'spec'    => 'int',     // special attack energy, 0-100
];

header('Content-Type: application/json');
header('Cache-Control: no-store');

function reply(int $status, array $body): void {
    http_response_code($status);
    echo json_encode($body);
    exit;
}

function isIntMatrix($v, int $maxRows, int $cols): bool {
    if (!is_array($v) || !array_is_list_compat($v) || count($v) > $maxRows) return false;
    foreach ($v as $row) {
        if (!is_array($row) || count($row) !== $cols) return false;
        foreach ($row as $n) {
            if (!is_int($n)) return false;
        }
    }
    return true;
}

// array_is_list() is PHP 8.1+, which the host has, but keep this explicit.
function array_is_list_compat(array $a): bool {
    return $a === [] || array_keys($a) === range(0, count($a) - 1);
}

function validPatch($patch): bool {
    if (!is_array($patch) || array_is_list_compat($patch)) return false;
    foreach ($patch as $key => $value) {
        if (!isset(SCHEMA[$key])) return false;
        $shape = SCHEMA[$key];
        if ($shape === 'int') {
            if (!is_int($value)) return false;
        } elseif (!isIntMatrix($value, $shape[0], $shape[1])) {
            return false;
        }
    }
    return true;
}

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    reply(200, ['ok' => true, 'service' => 'party']);
}
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    reply(405, ['ok' => false, 'error' => 'method']);
}

$raw = file_get_contents('php://input', false, null, 0, MAX_BODY + 1);
if ($raw === false || strlen($raw) > MAX_BODY) {
    reply(413, ['ok' => false, 'error' => 'too_large']);
}
$req = json_decode($raw, true);
if (!is_array($req)) {
    reply(400, ['ok' => false, 'error' => 'json']);
}

$room = $req['room'] ?? '';
$member = $req['member'] ?? '';
$name = $req['name'] ?? '';
$since = $req['since'] ?? 0;
$patch = $req['patch'] ?? null;
$leave = ($req['leave'] ?? false) === true;

if (!is_string($room) || !preg_match('/^[0-9a-f]{64}$/', $room)
    || !is_string($member) || !preg_match('/^[0-9a-f]{32}$/', $member)
    || !is_string($name) || !preg_match('/^[A-Za-z0-9 _-]{1,12}$/', $name)
    || !is_int($since) || $since < 0
    || ($patch !== null && !validPatch($patch))) {
    reply(400, ['ok' => false, 'error' => 'invalid']);
}

if (!is_dir(DATA_DIR) && !mkdir(DATA_DIR, 0700, true)) {
    reply(500, ['ok' => false, 'error' => 'storage']);
}

try {
    $db = new PDO('sqlite:' . DATA_DIR . '/party.sqlite', null, null, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_TIMEOUT => 5,
    ]);
    $db->exec('PRAGMA journal_mode=WAL');
    $db->exec('PRAGMA synchronous=NORMAL');
    $db->exec('CREATE TABLE IF NOT EXISTS members (
        room TEXT NOT NULL,
        member TEXT NOT NULL,
        name TEXT NOT NULL,
        data TEXT NOT NULL,
        seq INTEGER NOT NULL,
        updated INTEGER NOT NULL,
        PRIMARY KEY (room, member)
    )');
    $db->exec('CREATE INDEX IF NOT EXISTS members_updated ON members (updated)');
    // Global change counter, kept apart from members so it never goes
    // backwards when every member row expires.
    $db->exec('CREATE TABLE IF NOT EXISTS counter (id INTEGER PRIMARY KEY CHECK (id = 1), seq INTEGER NOT NULL)');
    $db->exec('INSERT OR IGNORE INTO counter (id, seq) VALUES (1, 0)');

    $now = time();
    $db->exec('BEGIN IMMEDIATE');

    $db->prepare('DELETE FROM members WHERE updated < ?')->execute([$now - STALE_SECONDS]);

    if ($leave) {
        $db->prepare('DELETE FROM members WHERE room = ? AND member = ?')->execute([$room, $member]);
        $db->exec('COMMIT');
        reply(200, ['ok' => true]);
    }

    $stmt = $db->prepare('SELECT name, data FROM members WHERE room = ? AND member = ?');
    $stmt->execute([$room, $member]);
    $self = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($self === false) {
        $stmt = $db->prepare('SELECT COUNT(*) FROM members WHERE room = ?');
        $stmt->execute([$room]);
        if ((int)$stmt->fetchColumn() >= MAX_MEMBERS) {
            $db->exec('ROLLBACK');
            reply(403, ['ok' => false, 'error' => 'room_full']);
        }
    }

    if ($self === false || $patch !== null || $self['name'] !== $name) {
        // Content changed: merge the patch and bump this member's seq so
        // other members pick it up on their next poll.
        $data = $self === false ? [] : json_decode($self['data'], true);
        if ($patch !== null) {
            $data = array_merge($data, $patch);
        }
        $db->exec('UPDATE counter SET seq = seq + 1 WHERE id = 1');
        $seq = (int)$db->query('SELECT seq FROM counter WHERE id = 1')->fetchColumn();
        $db->prepare('INSERT OR REPLACE INTO members (room, member, name, data, seq, updated)
                      VALUES (?, ?, ?, ?, ?, ?)')
           ->execute([$room, $member, $name, json_encode((object)$data), $seq, $now]);
    } else {
        // Heartbeat only.
        $db->prepare('UPDATE members SET updated = ? WHERE room = ? AND member = ?')
           ->execute([$now, $room, $member]);
    }

    $stmt = $db->prepare('SELECT member, name, data, seq FROM members WHERE room = ? AND member != ?');
    $stmt->execute([$room, $member]);
    $present = [];
    $changed = [];
    $maxSeq = $since;
    foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
        $present[] = $row['member'];
        $rowSeq = (int)$row['seq'];
        if ($rowSeq > $since) {
            $changed[] = [
                'member' => $row['member'],
                'name' => $row['name'],
                'state' => json_decode($row['data']),
            ];
        }
        $maxSeq = max($maxSeq, $rowSeq);
    }

    $db->exec('COMMIT');
    reply(200, ['ok' => true, 'seq' => $maxSeq, 'present' => $present, 'members' => $changed]);
} catch (Throwable $e) {
    if (isset($db) && $db->inTransaction()) {
        $db->exec('ROLLBACK');
    }
    error_log('party.php: ' . $e->getMessage());
    reply(500, ['ok' => false, 'error' => 'server']);
}
