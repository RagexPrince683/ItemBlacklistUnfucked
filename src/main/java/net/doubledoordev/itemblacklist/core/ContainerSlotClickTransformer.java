package net.doubledoordev.itemblacklist.core;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Adds the server-authoritative crafting-result guard to Container.slotClick. */
public final class ContainerSlotClickTransformer implements IClassTransformer
{
    private static final String TARGET_CLASS = "net.minecraft.inventory.Container";
    private static final String TARGET_METHOD_SRG = "func_75144_a";
    private static final String TARGET_METHOD_MCP = "slotClick";
    private static final String TARGET_DESCRIPTOR =
            "(IIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;";
    private static final String HANDLER_INTERNAL =
            "net/doubledoordev/itemblacklist/util/CraftingResultHandler";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (basicClass == null || !TARGET_CLASS.equals(transformedName)) return basicClass;

        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        MethodNode target = null;
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (MethodNode method : classNode.methods)
        {
            // classNode.name and method.desc are still fully obfuscated when
            // this coremod runs before Forge's runtime deobfuscating transformer.
            String mappedName = remapper.mapMethodName(classNode.name, method.name, method.desc);
            String mappedDescriptor = remapper.mapMethodDesc(method.desc);
            boolean expectedName = TARGET_METHOD_MCP.equals(method.name)
                    || TARGET_METHOD_SRG.equals(method.name)
                    || TARGET_METHOD_SRG.equals(mappedName)
                    || TARGET_METHOD_MCP.equals(mappedName);
            if (expectedName && TARGET_DESCRIPTOR.equals(mappedDescriptor))
            {
                target = method;
                break;
            }
        }

        if (target == null)
        {
            String message = "ItemBlacklist could not find Container.slotClick (slotClick/func_75144_a) "
                    + TARGET_DESCRIPTOR + "; banned crafting-result extraction cannot be protected.";
            FMLLog.severe(message);
            throw new IllegalStateException(message);
        }

        injectGuard(classNode.name, target);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static void injectGuard(String containerInternalName, MethodNode method)
    {
        Type[] arguments = Type.getArgumentTypes(method.desc);
        String handlerDescriptor = "(" + arguments[3].getDescriptor() + "L" + containerInternalName
                + ";I)Z";
        LabelNode continueVanilla = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 4));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new VarInsnNode(Opcodes.ILOAD, 1));
        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HANDLER_INTERNAL,
                "isBlockedCraftingResult", handlerDescriptor, false));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, continueVanilla));
        guard.add(new InsnNode(Opcodes.ACONST_NULL));
        guard.add(new InsnNode(Opcodes.ARETURN));
        guard.add(continueVanilla);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));

        AbstractInsnNode first = method.instructions.getFirst();
        method.instructions.insertBefore(first, guard);
    }
}
