package InterfaceDebugPlugin;

import plugin.Plugin;
import plugin.annotations.PluginMeta;
import plugin.api.*;
import rt4.Component;
import rt4.ComponentPointer;
import rt4.GameShell;
import rt4.InterfaceList;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class plugin extends Plugin {
    private boolean isEnabled;
    private boolean isVerbose;

    private final ArrayList<Integer> activeVarps = new ArrayList<>();

    @Override
    public void ComponentDraw(int componentIndex, Component component, int screenX, int screenY) {
        if (!isEnabled) return;

        FontColor color = new FontColor((((component.type * 50) << 16) | 255 << 8 | Math.min(component.type * 50, 255)));

        if (!isVerbose) {
            if
            (
                component.type != 0
                && component.type != 9
                && (component.type != 5
                        || (component.onOptionClick != null
                        || component.onMouseOver != null
                        || component.onHold != null
                        || component.varpTriggers != null
                        || component.onVarcstrTransmit != null
                        || component.onVarcTransmit != null
                        || component.cs1ComparisonOpcodes != null))
                || component.hidden
            ) return;
        }

        API.DrawText(
                FontType.SMALL,
                color,
                TextModifier.LEFT,
                componentIndex + "" + (component.modelId != 0 ? " (" +  component.modelId + ")" : ""),
                screenX,
                screenY
        );

        if (component.varpTriggers != null) {
            Arrays.stream(component.varpTriggers).forEach(varp -> { if(!activeVarps.contains(varp)) activeVarps.add(varp); });
        }
    }

    @Override
    public void Draw(long timeDelta) {
       if (!isEnabled) return;

       StringBuilder sb = new StringBuilder();
       sb.append("Varps: [");

       for (int varp : activeVarps) sb.append(varp).append(" ");

       sb.append("]");

       API.DrawText(
               FontType.SMALL,
               FontColor.YELLOW,
               TextModifier.CENTER,
               sb.toString(),
               GameShell.canvasWidth / 2 - 100,
               20
       );
    }

    @Override
    public void ProcessCommand(String commandStr, String[] args) {
        if (commandStr.equalsIgnoreCase("::debug_iface")){
            isEnabled = !isEnabled;
        }

        if (commandStr.equalsIgnoreCase("::debug_iface_verbose")) {
            isVerbose = !isVerbose;
        }

        if (commandStr.equalsIgnoreCase("::clear_iface_varps")) {
            activeVarps.clear();
        }

        if (commandStr.equalsIgnoreCase("::dumpinterface")) {
            if (args.length < 1) {
                API.SendMessage("Usage: ::dumpinterface <interfaceId>");
                return;
            }
            try {
                int interfaceId = Integer.parseInt(args[0]);
                dumpInterface(interfaceId);
            } catch (NumberFormatException e) {
                API.SendMessage("Invalid interface id: " + args[0]);
            }
        }

        if (commandStr.equalsIgnoreCase("::findhost")) {
            if (args.length < 1) {
                API.SendMessage("Usage: ::findhost <interfaceId>");
                return;
            }
            try {
                int interfaceId = Integer.parseInt(args[0]);
                findHost(interfaceId);
            } catch (NumberFormatException e) {
                API.SendMessage("Invalid interface id: " + args[0]);
            }
        }
    }

    private void findHost(int interfaceId) {
        StringBuilder sb = new StringBuilder();
        sb.append("topLevelInterface=").append(InterfaceList.topLevelInterface).append("\n");
        int matches = 0;
        for (ComponentPointer ptr = (ComponentPointer) InterfaceList.openInterfaces.head();
             ptr != null;
             ptr = (ComponentPointer) InterfaceList.openInterfaces.next()) {
            if (ptr.interfaceId == interfaceId) {
                long hostComponentId = ptr.key;
                int hostInterfaceId = (int) (hostComponentId >> 16);
                int hostComponentIndex = (int) (hostComponentId & 0xFFFF);
                sb.append("Hosted at componentId=").append(hostComponentId)
                        .append(" hostInterface=").append(hostInterfaceId)
                        .append(" hostComponentIndex=").append(hostComponentIndex)
                        .append("\n");
                Component[] hostArr = InterfaceList.components[hostInterfaceId];
                if (hostArr != null && hostComponentIndex < hostArr.length && hostArr[hostComponentIndex] != null) {
                    Component h = hostArr[hostComponentIndex];
                    sb.append("Host component: pos=(").append(h.x).append(",").append(h.y)
                            .append(") size=(").append(h.width).append("x").append(h.height)
                            .append(") type=").append(h.type)
                            .append("\n");
                }
                matches++;
            }
        }
        if (matches == 0) {
            sb.append("No open interface entry found hosting interface ").append(interfaceId);
        }
        try (PrintWriter out = new PrintWriter(new FileWriter("findhost_" + interfaceId + ".txt"))) {
            out.println(sb.toString());
            API.SendMessage("Wrote findhost_" + interfaceId + ".txt");
        } catch (Exception e) {
            API.SendMessage("findhost failed: " + e.getMessage());
        }
    }

    private void dumpInterface(int interfaceId) {
        Component[] top = InterfaceList.components[interfaceId];
        if (top == null) {
            API.SendMessage("Interface " + interfaceId + " is not loaded.");
            return;
        }
        try (PrintWriter out = new PrintWriter(new FileWriter("interface_dump_" + interfaceId + ".txt"))) {
            out.println("Interface " + interfaceId + " dump");
            for (int i = 0; i < top.length; i++) {
                dumpComponent(out, top[i], i, 0);
            }
            API.SendMessage("Dumped interface " + interfaceId + " to interface_dump_" + interfaceId + ".txt");
        } catch (Exception e) {
            API.SendMessage("Dump failed: " + e.getMessage());
        }
    }

    private void dumpComponent(PrintWriter out, Component c, int index, int depth) {
        if (c == null) return;
        String indent = repeat("  ", depth);
        out.println(indent + "[" + index + "] id=" + c.id
                + " type=" + c.type
                + " overlayer=" + c.overlayer
                + " clientCode=" + c.clientCode
                + " pos=(" + c.x + "," + c.y + ")"
                + " size=(" + c.width + "x" + c.height + ")"
                + " hidden=" + c.hidden
                + " scroll=(" + c.scrollX + "," + c.scrollY + ") scrollMaxV=" + c.scrollMaxV
                + " hasOnScroll=" + (c.onScroll != null)
                + " noClickThrough=" + c.noClickThrough
        );
        if (c.createdComponents != null) {
            out.println(indent + "  -- createdComponents (" + c.createdComponents.length + ") --");
            for (int j = 0; j < c.createdComponents.length; j++) {
                dumpComponent(out, c.createdComponents[j], j, depth + 2);
            }
        }
    }

    private String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(s);
        return sb.toString();
    }
}
