package SpaceContinue

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import rt4.Component
import rt4.ComponentPointer
import rt4.Cs1ScriptRunner
import rt4.InterfaceList
import rt4.MiniMenu
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

// Lets Space advance "Click here to continue" dialogue prompts, and number
// keys 1-9 pick a choice on a "Select an Option" prompt, instead of
// requiring a mouse click every time.
//
// Traced the real mechanism from MiniMenu.java rather than guessing: a
// component gets the "Continue" menu option added (action code 41, still
// unnamed as UNKNOWN_41 in this deobfuscation) only when
// ServerActiveProperties.isResumePauseButtonEnabled() is true for it - a
// single server-set bit (events & 0x1). Clicking that option runs, in
// MiniMenu.doAction():
//
//     sendComponentClickPacket(menuArg1, menuArg2);
//     Cs1ScriptRunner.pleaseWaitComponent = InterfaceList.getComponent(menuArg2, menuArg1);
//     InterfaceList.redraw(Cs1ScriptRunner.pleaseWaitComponent);
//
// where menuArg1/menuArg2 are just the component's own createdComponentId
// and id. A "Select an Option" prompt (confirmed live via ::dumpinterface
// against interface 234) uses this exact same mechanism, just with one
// sibling text component per choice line instead of a single one.
//
// First version of this scanned ALL of InterfaceList.components - every
// interface the client has ever loaded into memory, not just what's
// actually on screen right now. Fixed by only scanning interfaces that are
// actually part of the currently-displayed tree: the top-level interface
// plus every interface currently docked via InterfaceList.openInterfaces.
//
// v1.3/v1.4 chased the wrong theory (ambiguity with normal chat typing) and
// tried gating behind Ctrl, then behind a side-effect-free diagnostic. The
// diagnostic build crashed too, which was the real clue: even a READ-ONLY
// scan was enough to crash it, regardless of Ctrl or whether anything
// matched. That's because the crash was never about which component gets
// matched - it's a thread-safety bug. keyPressed() runs on Java's AWT event
// thread, but InterfaceList.getServerActiveProperties() reads a shared,
// non-thread-safe HashTable (InterfaceList.properties) that the MAIN GAME
// THREAD is also constantly reading/writing every single frame (via
// MiniMenu.addComponentEntries, building hover menus). HashTable.get() uses
// shared mutable cursor state with no locking - two threads touching it at
// the same instant corrupts its internal linked list, and the next read
// from either thread (often the game thread, moments later, somewhere
// completely unrelated) throws a NullPointerException deep inside engine
// code. Confirmed live via a captured stack trace:
//   NullPointerException at rt4.HashTable.get:98, called from
//   InterfaceList.getServerActiveProperties -> InterfaceList.getOp ->
//   MiniMenu.addComponentEntries -> ... -> GameShell.run (the main loop) -
//   nowhere in this plugin's own call stack, because the corruption and
//   the crash happen on different threads at different moments.
//
// The client's own native input handling already avoids exactly this trap:
// Keyboard.keyPressed() (also AWT-thread) never touches interface state -
// it only pushes raw key codes into a plain int array. All the actual
// interface work happens later, once per frame, on the main game thread.
// v1.5 follows the same pattern: keyPressed() only records which key was
// pressed (a plain field write, nothing shared with the render loop), and
// the actual scan + click happens in Draw(), which the client calls once
// per frame from the main thread - so it can never race the game loop's
// own use of the same HashTable again. This is what makes bare Space/1-9
// safe to restore, with no modifier and no behavior change from the
// original design.
@PluginMeta(
        author = "OblongNoodle",
        description = "Press space to continue dialogue, or 1-9 to pick a 'Select an Option' choice.",
        version = 1.5
)
class plugin : Plugin() {
    @Volatile private var pendingContinue = false
    @Volatile private var pendingChoiceIndex = -1

    override fun Init() {
        API.AddKeyboardListener(object : KeyAdapter() {
            // Only ever touches these two plain fields here - never
            // InterfaceList/MiniMenu/Cs1ScriptRunner. Those are only safe
            // to touch from the main game thread, in Draw() below.
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_SPACE) {
                    pendingContinue = true
                } else if (e.keyCode in KeyEvent.VK_1..KeyEvent.VK_9) {
                    pendingChoiceIndex = e.keyCode - KeyEvent.VK_1
                }
            }
        })
    }

    override fun Draw(timeDelta: Long) {
        try {
            if (pendingContinue) {
                pendingContinue = false
                tryContinue()
            }
            if (pendingChoiceIndex >= 0) {
                val index = pendingChoiceIndex
                pendingChoiceIndex = -1
                tryChooseOption(index)
            }
        } catch (ex: Exception) {
            // A keyboard shortcut must never be able to take the whole
            // client down - best-effort only.
        }
    }

    private fun tryContinue() {
        // Matches doAction's own guard - never fire while a request is
        // already in flight for a previous click.
        if (Cs1ScriptRunner.pleaseWaitComponent != null) return
        val options = findResumePauseComponents()
        // Only fires for a genuine single "click here to continue" - with
        // multiple choices on screen, space is ambiguous about which one is
        // meant, so a number key is required instead.
        if (options.size != 1) return
        click(options[0])
    }

    private fun tryChooseOption(index: Int) {
        if (Cs1ScriptRunner.pleaseWaitComponent != null) return
        val target = findResumePauseComponents().getOrNull(index) ?: return
        click(target)
    }

    private fun click(target: Component) {
        MiniMenu.sendComponentClickPacket(target.createdComponentId, target.id)
        Cs1ScriptRunner.pleaseWaitComponent = InterfaceList.getComponent(target.id, target.createdComponentId)
        InterfaceList.redraw(Cs1ScriptRunner.pleaseWaitComponent)
    }

    // Only the top-level interface and whatever's currently docked into it
    // (InterfaceList.openInterfaces) - NOT the full InterfaceList.components
    // array, which includes interfaces merely loaded into memory but not
    // actually part of what's on screen right now.
    private fun currentlyDisplayedInterfaceIds(): Set<Int> {
        val ids = mutableSetOf<Int>()
        val top = InterfaceList.topLevelInterface
        if (top != -1) ids.add(top)
        var ptr = InterfaceList.openInterfaces.head() as? ComponentPointer
        while (ptr != null) {
            ids.add(ptr.interfaceId)
            ptr = InterfaceList.openInterfaces.next() as? ComponentPointer
        }
        return ids
    }

    private fun findResumePauseComponents(): List<Component> {
        val allInterfaces = InterfaceList.components ?: return emptyList()
        val relevantIds = currentlyDisplayedInterfaceIds()
        val found = mutableListOf<Component>()
        for (id in relevantIds) {
            val components = allInterfaces.getOrNull(id) ?: continue
            for (component in components) {
                component ?: continue
                if (component.hidden) continue
                if (InterfaceList.getServerActiveProperties(component)?.isResumePauseButtonEnabled() == true) {
                    found.add(component)
                }
            }
        }
        return found.sortedBy { it.id }
    }
}
