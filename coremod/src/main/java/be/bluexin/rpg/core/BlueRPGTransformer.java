/*
 * Copyright (C) 2019.  Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package be.bluexin.rpg.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class BlueRPGTransformer implements IClassTransformer, Opcodes {

    private static final String ENTITY_PLAYER = "Lnet/minecraft/entity/player/EntityPlayer;";
    private static final String WORLD = "Lnet/minecraft/world/World;";

    private static final String ASM_HOOKS = "be/bluexin/rpg/core/BlueRPGHooks";
    private static final Map<String, Transformer> transformers = new HashMap<>();

    static {
        transformers.put("net.minecraft.entity.player.EntityPlayer", BlueRPGTransformer::transformPlayer);
        transformers.put("net.minecraft.entity.EntityLivingBase", BlueRPGTransformer::transformELB);
        transformers.put("net.minecraft.block.BlockEnderChest", BlueRPGTransformer::transformBEC);
        transformers.put("boni.dummy.EntityDummy", BlueRPGTransformer::transformDummy);
        transformers.put("net.minecraft.entity.player.InventoryPlayer", BlueRPGTransformer::transformInventoryPlayer);
    }

    private static byte[] transformPlayer(byte[] basicClass) {
        final String CONTAINER = "Lnet/minecraft/inventory/Container;";
        final String INVENTORY_PLAYER = "Lnet/minecraft/entity/player/InventoryPlayer;";
        final String GAME_PROFILE = "Lcom/mojang/authlib/GameProfile;";

        final MethodSignature init = new MethodSignature("<init>", "<init>", "(" + WORLD + GAME_PROFILE + ")V");
        final FieldSignature inventoryPlayer = new FieldSignature("inventory", "field_71071_by", INVENTORY_PLAYER);
        final FieldSignature inventoryContainer = new FieldSignature("inventoryContainer", "field_71069_bz", CONTAINER);

        return transform(basicClass, init, "Player Inventory & Container Hooks", all(
                combine(
                        (node) -> node.getOpcode() == PUTFIELD && inventoryPlayer.matches((FieldInsnNode) node),
                        (method, node) -> {
                            InsnList l = new InsnList();
                            l.add(new VarInsnNode(ALOAD, 0));
                            l.add(new MethodInsnNode(INVOKESTATIC, ASM_HOOKS, "inventoryPlayerHook",
                                    "(" + INVENTORY_PLAYER + ENTITY_PLAYER + ")" + INVENTORY_PLAYER, false));

                            method.instructions.insertBefore(node, l);

                            return true;
                        }
                ),
                combine(
                        (node) -> node.getOpcode() == PUTFIELD && inventoryContainer.matches((FieldInsnNode) node),
                        (method, node) -> {
                            final String CONTAINER_PLAYER = "Lnet/minecraft/inventory/ContainerPlayer;";

                            InsnList l = new InsnList();
                            l.add(new VarInsnNode(ALOAD, 0));
                            l.add(new MethodInsnNode(INVOKESTATIC, ASM_HOOKS, "containerPlayerHook",
                                    "(" + CONTAINER_PLAYER + ENTITY_PLAYER + ")" + CONTAINER, false));

                            method.instructions.insertBefore(node, l);

                            return true;
                        }
                )
        ));
    }

    private static byte[] transformELB(byte[] basicClass) {
        // Lnet/minecraft/entity/EntityLivingBase;onUpdate()V
        final MethodSignature onUpdate = new MethodSignature("onUpdate", "func_70071_h_", "()V");
        // net/minecraft/entity/EntityLivingBase$1.$SwitchMap$net$minecraft$inventory$EntityEquipmentSlot$Type : [I
        final FieldSignature switchLookup = new FieldSignature(
                "$SwitchMap$net$minecraft$inventory$EntityEquipmentSlot$Type",
                "$SwitchMap$net$minecraft$inventory$EntityEquipmentSlot$Type",
                "[I"
        );
        // Lnet/minecraft/entity/EntityLivingBase;sendEnterCombat()V
        final MethodSignature startCombat = new MethodSignature("sendEnterCombat", "func_152111_bt", "()V");
        // Lnet/minecraft/entity/EntityLivingBase;sendEndCombat()V
        final MethodSignature endCombat = new MethodSignature("sendEndCombat", "func_152112_bu", "()V");
        final MethodSignature init = new MethodSignature("<init>", "<init>", "(" + WORLD + ")V");
        final String COMBAT_TRACKER = "Lnet/minecraft/util/CombatTracker;";
        final FieldSignature combatTracker = new FieldSignature("combatTracker", "field_94063_bt", COMBAT_TRACKER);

        return transform(transform(transform(transform(basicClass,
                onUpdate, "EntityLivingBase post equipment change hook", combine(
                        (node) -> {
                            boolean ok = node.getOpcode() == GETSTATIC && switchLookup.matches((FieldInsnNode) node);
                            if (!ok) return false;

                            ok = false;
                            AbstractInsnNode prev = node;
                            for (int i = 0; !ok && i < 5; i++) {
                                prev = prev.getPrevious();
                                ok = prev.getOpcode() == INVOKEVIRTUAL;
                            }
                            return ok;
                        },
                        (method, node) -> {
                            InsnList l = new InsnList();
                            LabelNode newLabel = new LabelNode();
                            l.add(newLabel);
                            l.add(new VarInsnNode(ALOAD, 0));
                            l.add(new VarInsnNode(ALOAD, 5));
                            l.add(new MethodInsnNode(INVOKESTATIC, ASM_HOOKS, "equipmentPostChangeHook",
                                    "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/inventory/EntityEquipmentSlot;)V", false));

                            AbstractInsnNode next = node.getNext();
                            while (!(next instanceof LookupSwitchInsnNode)) next = next.getNext();

                            LookupSwitchInsnNode sw = (LookupSwitchInsnNode) next;
                            LabelNode oldLabel = sw.dflt;
                            sw.dflt = newLabel;

                            method.instructions.insertBefore(oldLabel, l);

                            while (!(next instanceof LabelNode) || ((LabelNode) next).getLabel() != oldLabel.getLabel()) {
                                if (next instanceof JumpInsnNode) {
                                    JumpInsnNode j = ((JumpInsnNode) next);
                                    if (j.label.getLabel() == oldLabel.getLabel()) j.label = newLabel;
                                }
                                next = next.getNext();
                            }

                            return true;
                        })),
                startCombat, "EntityLivingBase start combat hook", combine(
                        (node) -> node.getOpcode() == RETURN,
                        (method, node) -> {
                            final InsnList l = new InsnList();
                            l.add(new VarInsnNode(ALOAD, 0));
                            l.add(new MethodInsnNode(INVOKESTATIC, ASM_HOOKS, "combatStart",
                                    "(Lnet/minecraft/entity/EntityLivingBase;)V", false));

                            method.instructions.insertBefore(node, l);

                            return true;
                        })),
                endCombat, "EntityLivingBase end combat hook", combine(
                        (node) -> node.getOpcode() == RETURN,
                        (method, node) -> {
                            final InsnList l = new InsnList();
                            l.add(new VarInsnNode(ALOAD, 0));
                            l.add(new MethodInsnNode(INVOKESTATIC, ASM_HOOKS, "combatEnd",
                                    "(Lnet/minecraft/entity/EntityLivingBase;)V", false));

                            method.instructions.insertBefore(node, l);

                            return true;
                        })),
                init, "EntityLivingBase CombatTracker replacement", combine(
                        (node) -> node.getOpcode() == PUTFIELD && combatTracker.matches((FieldInsnNode) node),
                        (method, node) -> {
                            InsnList l = new InsnList();
                            l.add(new VarInsnNode(ALOAD, 0));
                            l.add(new MethodInsnNode(INVOKESTATIC, ASM_HOOKS, "combatTrackerHook",
                                    "(" + COMBAT_TRACKER + "Lnet/minecraft/entity/EntityLivingBase;)" + COMBAT_TRACKER, false));

                            method.instructions.insertBefore(node, l);

                            return true;
                        }
                )
        );
    }

    private static byte[] transformBEC(byte[] basicClass) {
        // func_180639_a(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;Lnet/minecraft/util/EnumFacing;FFF)Z
        final MethodSignature onBlockActivated = new MethodSignature("onBlockActivated", "func_180639_a", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;Lnet/minecraft/util/EnumFacing;FFF)Z");
        final MethodSignature displayGUIChest = new MethodSignature("displayGUIChest", "func_71007_a", "(Lnet/minecraft/inventory/IInventory;)V");

        return transform(basicClass, onBlockActivated, "BlockEnderChest openGUI", combine(
                (node) -> node.getOpcode() == INVOKEVIRTUAL && displayGUIChest.matches((MethodInsnNode) node),
                (method, node) -> {
                    InsnList l = new InsnList();
                    l.add(new MethodInsnNode(INVOKESTATIC, ASM_HOOKS, "openEnderChestGui",
                            "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/inventory/IInventory;)V", false));

                    method.instructions.insertBefore(node, l);
                    method.instructions.remove(node);

                    return true;
                }
        ));
    }

    private static byte[] transformDummy(byte[] basicClass) {
        final MethodSignature damageEntity = new MethodSignature("attackEntityFrom", "func_70097_a", "(Lnet/minecraft/util/DamageSource;F)Z");
        final FieldSignature hurtResistantTime = new FieldSignature("hurtResistantTime", "hurtResistantTime", "I");

        return transform(basicClass, damageEntity, "Dummy attack hook", combine(
                (node) -> node.getOpcode() == GETFIELD && hurtResistantTime.matches((FieldInsnNode) node),
                (method, node) -> {
                    while (node.getOpcode() != IRETURN) node = node.getPrevious();
                    node = node.getNext();

                    final InsnList l = new InsnList();
                    final LabelNode label = new LabelNode();

                    l.add(new VarInsnNode(ALOAD, 0));
                    l.add(new VarInsnNode(ALOAD, 1));
                    l.add(new VarInsnNode(FLOAD, 2));
                    l.add(new MethodInsnNode(INVOKESTATIC, "net/minecraftforge/common/ForgeHooks", "onLivingAttack",
                            "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;F)Z", false));
                    l.add(new JumpInsnNode(IFNE, label));
                    l.add(new InsnNode(ICONST_0));
                    l.add(new InsnNode(IRETURN));
                    l.add(label);

                    method.instructions.insert(node, l);

                    return true;
                }
        ));
    }

    private static byte[] transformInventoryPlayer(byte[] basicClass) {
        final MethodSignature getHotbarSize = new MethodSignature("getHotbarSize", "func_70451_h", "()I");
        final MethodSignature isHotbar = new MethodSignature("isHotbar", "func_184435_e", "(I)Z");

        return transform(transform(basicClass, getHotbarSize, "Change static getHotbarSize", combine(
                (node) -> node.getOpcode() == BIPUSH,
                (method, node) -> {
                    ((IntInsnNode) node).operand = 4;
                    return true;
                })), isHotbar, "Change static isHotbar", combine(
                (node) -> node.getOpcode() == BIPUSH,
                (method, node) -> {
                    ((IntInsnNode) node).operand = 4;
                    return true;
                }
        ));
    }

    // BOILERPLATE =====================================================================================================

    public static byte[] transform(byte[] basicClass, MethodSignature sig, String simpleDesc, MethodAction action) {
        ClassReader reader = new ClassReader(basicClass);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        log("Applying Transformation to method (" + sig + ")");
        log("Attempting to insert: " + simpleDesc);
        boolean didAnything = findMethodAndTransform(node, sig, action);

        if (didAnything) {
            ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            return writer.toByteArray();
        }

        return basicClass;
    }

    public static boolean findMethodAndTransform(ClassNode node, MethodSignature sig, MethodAction pred) {
        for (MethodNode method : node.methods) {
            if (sig.matches(method)) {

                boolean finish = pred.test(method);
                log("Patch result: " + (finish ? "Success" : "!!!!!!! Failure !!!!!!!"));

                return finish;
            }
        }

        log("Patch result: !!!!!!! Couldn't locate method! !!!!!!!");

        return false;
    }

    public static MethodAction combine(NodeFilter filter, NodeAction action) {
        return (MethodNode node) -> applyOnNode(node, filter, action);
    }

    public static MethodAction all(MethodAction... actions) {
        return (MethodNode node) -> {
            boolean didAny = true;
            for (MethodAction a : actions) {
                didAny &= a.test(node);
            }
            return didAny;
        };
    }

    public static boolean applyOnNode(MethodNode method, NodeFilter filter, NodeAction action) {
        AbstractInsnNode[] nodes = method.instructions.toArray();
        Iterator<AbstractInsnNode> iterator = new InsnArrayIterator(nodes);

        boolean didAny = false;
        while (iterator.hasNext()) {
            AbstractInsnNode anode = iterator.next();
            if (filter.test(anode)) {
                didAny = true;
                if (action.test(method, anode))
                    break;
            }
        }

        return didAny;
    }

    public static MethodAction combineByLast(NodeFilter filter, NodeAction action) {
        return (MethodNode node) -> applyOnNodeByLast(node, filter, action);
    }

    public static boolean applyOnNodeByLast(MethodNode method, NodeFilter filter, NodeAction action) {
        AbstractInsnNode[] nodes = method.instructions.toArray();
        ListIterator<AbstractInsnNode> iterator = new InsnArrayIterator(nodes, method.instructions.size());

        boolean didAny = false;
        while (iterator.hasPrevious()) {
            AbstractInsnNode anode = iterator.previous();
            if (filter.test(anode)) {
                didAny = true;
                if (action.test(method, anode))
                    break;
            }
        }

        return didAny;
    }

    public static MethodAction combineFrontPivot(NodeFilter pivot, NodeFilter filter, NodeAction action) {
        return (MethodNode node) -> applyOnNodeFrontPivot(node, pivot, filter, action);
    }

    public static boolean applyOnNodeFrontPivot(MethodNode method, NodeFilter pivot, NodeFilter filter, NodeAction action) {
        AbstractInsnNode[] nodes = method.instructions.toArray();
        ListIterator<AbstractInsnNode> iterator = new InsnArrayIterator(nodes);

        int pos = 0;

        boolean didAny = false;
        while (iterator.hasNext()) {
            pos++;
            AbstractInsnNode pivotTest = iterator.next();
            if (pivot.test(pivotTest)) {
                ListIterator<AbstractInsnNode> internal = new InsnArrayIterator(nodes, pos);
                while (internal.hasPrevious()) {
                    AbstractInsnNode anode = internal.previous();
                    if (filter.test(anode)) {
                        didAny = true;
                        if (action.test(method, anode))
                            break;
                    }
                }
            }
        }

        return didAny;
    }

    public static MethodAction combineBackPivot(NodeFilter pivot, NodeFilter filter, NodeAction action) {
        return (MethodNode node) -> applyOnNodeBackPivot(node, pivot, filter, action);
    }

    public static boolean applyOnNodeBackPivot(MethodNode method, NodeFilter pivot, NodeFilter filter, NodeAction action) {
        AbstractInsnNode[] nodes = method.instructions.toArray();
        ListIterator<AbstractInsnNode> iterator = new InsnArrayIterator(nodes, method.instructions.size());

        int pos = method.instructions.size();

        boolean didAny = false;
        while (iterator.hasPrevious()) {
            pos--;
            AbstractInsnNode pivotTest = iterator.previous();
            if (pivot.test(pivotTest)) {
                ListIterator<AbstractInsnNode> internal = new InsnArrayIterator(nodes, pos);
                while (internal.hasNext()) {
                    AbstractInsnNode anode = internal.next();
                    if (filter.test(anode)) {
                        didAny = true;
                        if (action.test(method, anode))
                            break;
                    }
                }
            }
        }

        return didAny;
    }

    public static MethodAction combineFrontFocus(NodeFilter focus, NodeFilter filter, NodeAction action) {
        return (MethodNode node) -> applyOnNodeFrontFocus(node, focus, filter, action);
    }

    public static boolean applyOnNodeFrontFocus(MethodNode method, NodeFilter focus, NodeFilter filter, NodeAction action) {
        AbstractInsnNode[] nodes = method.instructions.toArray();
        ListIterator<AbstractInsnNode> iterator = new InsnArrayIterator(nodes);

        int pos = method.instructions.size();

        boolean didAny = false;
        while (iterator.hasNext()) {
            pos++;
            AbstractInsnNode focusTest = iterator.next();
            if (focus.test(focusTest)) {
                ListIterator<AbstractInsnNode> internal = new InsnArrayIterator(nodes, pos);
                while (internal.hasNext()) {
                    AbstractInsnNode anode = internal.next();
                    if (filter.test(anode)) {
                        didAny = true;
                        if (action.test(method, anode))
                            break;
                    }
                }
            }
        }

        return didAny;
    }

    public static MethodAction combineBackFocus(NodeFilter focus, NodeFilter filter, NodeAction action) {
        return (MethodNode node) -> applyOnNodeBackFocus(node, focus, filter, action);
    }

    public static boolean applyOnNodeBackFocus(MethodNode method, NodeFilter focus, NodeFilter filter, NodeAction action) {
        AbstractInsnNode[] nodes = method.instructions.toArray();
        ListIterator<AbstractInsnNode> iterator = new InsnArrayIterator(nodes, method.instructions.size());

        int pos = method.instructions.size();

        boolean didAny = false;
        while (iterator.hasPrevious()) {
            pos--;
            AbstractInsnNode focusTest = iterator.previous();
            if (focus.test(focusTest)) {
                ListIterator<AbstractInsnNode> internal = new InsnArrayIterator(nodes, pos);
                while (internal.hasPrevious()) {
                    AbstractInsnNode anode = internal.previous();
                    if (filter.test(anode)) {
                        didAny = true;
                        if (action.test(method, anode))
                            break;
                    }
                }
            }
        }

        return didAny;
    }

    public static void log(String str) {
        LogManager.getLogger("BlueRPG ASM").info(str);
    }

    public static void prettyPrint(MethodNode node) {
        Printer printer = new Textifier();

        TraceMethodVisitor visitor = new TraceMethodVisitor(printer);
        node.accept(visitor);

        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();

        log(sw.toString());
    }

    public static void prettyPrint(AbstractInsnNode node) {
        Printer printer = new Textifier();

        TraceMethodVisitor visitor = new TraceMethodVisitor(printer);
        node.accept(visitor);

        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();

        log(sw.toString());
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformers.containsKey(transformedName)) {
            String[] arr = transformedName.split("\\.");
            log("Transforming " + arr[arr.length - 1]);
            return transformers.get(transformedName).apply(basicClass);
        }

        return basicClass;
    }

    public interface Transformer extends Function<byte[], byte[]> {
        // NO-OP
    }

    public interface MethodAction extends Predicate<MethodNode> {
        // NO-OP
    }

    // Basic interface aliases to not have to clutter up the code with generics over and over again

    public interface NodeFilter extends Predicate<AbstractInsnNode> {
        // NO-OP
    }

    public interface NodeAction extends BiPredicate<MethodNode, AbstractInsnNode> {
        // NO-OP
    }

    private static class InsnArrayIterator implements ListIterator<AbstractInsnNode> {

        private final AbstractInsnNode[] array;
        private int index;

        public InsnArrayIterator(AbstractInsnNode[] array) {
            this(array, 0);
        }

        public InsnArrayIterator(AbstractInsnNode[] array, int index) {
            this.array = array;
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return array.length > index + 1 && index >= 0;
        }

        @Override
        public AbstractInsnNode next() {
            if (hasNext())
                return array[++index];
            return null;
        }

        @Override
        public boolean hasPrevious() {
            return index > 0 && index <= array.length;
        }

        @Override
        public AbstractInsnNode previous() {
            if (hasPrevious())
                return array[--index];
            return null;
        }

        @Override
        public int nextIndex() {
            return hasNext() ? index + 1 : array.length;
        }

        @Override
        public int previousIndex() {
            return hasPrevious() ? index - 1 : 0;
        }

        @Override
        public void remove() {
            throw new Error("Unimplemented");
        }

        @Override
        public void set(AbstractInsnNode e) {
            throw new Error("Unimplemented");
        }

        @Override
        public void add(AbstractInsnNode e) {
            throw new Error("Unimplemented");
        }
    }

    public static class MethodSignature {
        private final String funcName, srgName, funcDesc;

        public MethodSignature(String funcName, String srgName, String funcDesc) {
            this.funcName = funcName;
            this.srgName = srgName;
            this.funcDesc = funcDesc;
        }

        @Override
        public String toString() {
            return "Names [" + funcName + ", " + srgName + "] Descriptor " + funcDesc;
        }

        public boolean matches(String methodName, String methodDesc) {
            return (methodName.equals(funcName) || methodName.equals(srgName))
                    && (methodDesc.equals(funcDesc));
        }

        public boolean matches(MethodNode method) {
            return matches(method.name, method.desc);
        }

        public boolean matches(MethodInsnNode method) {
            return matches(method.name, method.desc);
        }

    }

    public static class FieldSignature {
        private final String fieldName, srgName, fieldDesc;

        public FieldSignature(String fieldName, String srgName, String fieldDesc) {
            this.fieldName = fieldName;
            this.srgName = srgName;
            this.fieldDesc = fieldDesc;
        }

        @Override
        public String toString() {
            return "Names [" + fieldName + ", " + srgName + "] Descriptor " + fieldDesc;
        }

        public boolean matches(String fieldName, String fieldDesc) {
            return (fieldName.equals(this.fieldName) || fieldName.equals(srgName))
                    && (fieldDesc.equals(this.fieldDesc));
        }

        public boolean matches(FieldInsnNode field) {
            return matches(field.name, field.desc);
        }

        public boolean matches(FieldNode field) {
            return matches(field.name, field.desc);
        }
    }

    /**
     * Ugly workaround, I hate this
     */
    public static class SafeClassWriter extends ClassWriter {
        public SafeClassWriter(int flags) {
            super(flags);
        }

        public SafeClassWriter(ClassReader classReader, int flags) {
            super(classReader, flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return "java/lang/Object";
        }
    }

}
