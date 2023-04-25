package at.petrak.hexcasting.api.casting.circles;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.misc.FrozenColorizer;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.lib.HexItems;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * See {@link BlockEntityAbstractImpetus}, this is what's stored in it
 */
public class CircleExecutionState {
    public static final String
        TAG_IMPETUS_POS = "impetus_pos",
        TAG_IMPETUS_DIR = "impetus_dir",
        TAG_KNOWN_POSITIONS = "known_positions",
        TAG_REACHED_POSITIONS = "reached_positions",
        TAG_CURRENT_POS = "current_pos",
        TAG_ENTERED_FROM = "entered_from",
        TAG_IMAGE = "image",
        TAG_CASTER = "caster",
        TAG_COLORIZER = "colorizer";

    public final BlockPos impetusPos;
    public final Direction impetusDir;
    // Does contain the starting impetus
    public final Set<BlockPos> knownPositions;
    public final List<BlockPos> reachedPositions;
    public BlockPos currentPos;
    public Direction enteredFrom;
    public CastingImage currentImage;
    public @Nullable UUID caster;
    public FrozenColorizer colorizer;

    public final AABB bounds;


    protected CircleExecutionState(BlockPos impetusPos, Direction impetusDir, Set<BlockPos> knownPositions,
        List<BlockPos> reachedPositions, BlockPos currentPos, Direction enteredFrom,
        CastingImage currentImage, @Nullable UUID caster, FrozenColorizer colorizer) {
        this.impetusPos = impetusPos;
        this.impetusDir = impetusDir;
        this.knownPositions = knownPositions;
        this.reachedPositions = reachedPositions;
        this.currentPos = currentPos;
        this.enteredFrom = enteredFrom;
        this.currentImage = currentImage;
        this.caster = caster;
        this.colorizer = colorizer;

        this.bounds = BlockEntityAbstractImpetus.getBounds(new ArrayList<>(this.knownPositions));
    }

    public @Nullable ServerPlayer getCaster(ServerLevel world) {
        if (this.caster == null) {
            return null;
        }
        var entity = world.getEntity(this.caster);
        if (entity instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        // there's a problem if this branch is reached
        return null;
    }

    // Return null if the circle does not close.
    public static @Nullable CircleExecutionState createNew(BlockEntityAbstractImpetus impetus,
        @Nullable ServerPlayer caster) {
        var level = (ServerLevel) impetus.getLevel();

        if (level == null)
            return null;

        // Flood fill! Just like VCC all over again.
        // this contains tentative positions and directions entered from
        var todo = new Stack<Pair<Direction, BlockPos>>();
        todo.add(Pair.of(impetus.getStartDirection(), impetus.getBlockPos().relative(impetus.getStartDirection())));
        var seenPositions = new HashSet<BlockPos>();
        var seenGoodPositions = new ArrayList<BlockPos>();

        while (!todo.isEmpty()) {
            var pair = todo.pop();
            var enterDir = pair.getFirst();
            var herePos = pair.getSecond();

            if (seenPositions.add(herePos)) {
                // it's new
                var hereBs = level.getBlockState(herePos);
                if (!(hereBs.getBlock() instanceof ICircleComponent cmp)) {
                    continue;
                }

                if (!cmp.canEnterFromDirection(enterDir, herePos, hereBs, level)) {
                    continue;
                }

                seenGoodPositions.add(herePos);
                var outs = cmp.possibleExitDirections(herePos, hereBs, level);
                for (var out : outs) {
                    todo.add(Pair.of(out, herePos.relative(out)));
                }
            }
        }

        if (!seenPositions.contains(impetus.getBlockPos()) || seenGoodPositions.isEmpty()) {
            return null;
        }

        var knownPositions = new HashSet<>(seenGoodPositions);
        var reachedPositions = new ArrayList<BlockPos>();
        reachedPositions.add(impetus.getBlockPos());
        var start = seenGoodPositions.get(0);

        FrozenColorizer colorizer;
        UUID casterUUID;
        if (caster == null) {
            colorizer = new FrozenColorizer(new ItemStack(HexItems.DYE_COLORIZERS.get(DyeColor.PURPLE)),
                Util.NIL_UUID);
            casterUUID = null;
        } else {
            colorizer = HexAPI.instance().getColorizer(caster);
            casterUUID = caster.getUUID();
        }
        return new CircleExecutionState(impetus.getBlockPos(), impetus.getStartDirection(), knownPositions,
            reachedPositions, start, impetus.getStartDirection(), new CastingImage(), casterUUID, colorizer);
    }

    public CompoundTag save() {
        var out = new CompoundTag();

        out.put(TAG_IMPETUS_POS, NbtUtils.writeBlockPos(this.impetusPos));
        out.putByte(TAG_IMPETUS_DIR, (byte) this.impetusDir.ordinal());

        var knownTag = new ListTag();
        for (var bp : this.knownPositions) {
            knownTag.add(NbtUtils.writeBlockPos(bp));
        }
        out.put(TAG_KNOWN_POSITIONS, knownTag);

        var reachedTag = new ListTag();
        for (var bp : this.reachedPositions) {
            reachedTag.add(NbtUtils.writeBlockPos(bp));
        }
        out.put(TAG_REACHED_POSITIONS, reachedTag);

        out.put(TAG_CURRENT_POS, NbtUtils.writeBlockPos(this.currentPos));
        out.putByte(TAG_ENTERED_FROM, (byte) this.enteredFrom.ordinal());
        out.put(TAG_IMAGE, this.currentImage.serializeToNbt());

        if (this.caster != null)
            out.putUUID(TAG_CASTER, this.caster);

        out.put(TAG_COLORIZER, this.colorizer.serializeToNBT());

        return out;
    }

    public static CircleExecutionState load(CompoundTag nbt, ServerLevel world) {
        var startPos = NbtUtils.readBlockPos(nbt.getCompound(TAG_IMPETUS_POS));
        var startDir = Direction.values()[nbt.getByte(TAG_IMPETUS_DIR)];

        var knownPositions = new HashSet<BlockPos>();
        var knownTag = nbt.getList(TAG_KNOWN_POSITIONS, Tag.TAG_COMPOUND);
        for (var tag : knownTag) {
            knownPositions.add(NbtUtils.readBlockPos(HexUtils.downcast(tag, CompoundTag.TYPE)));
        }
        var reachedPositions = new ArrayList<BlockPos>();
        var reachedTag = nbt.getList(TAG_REACHED_POSITIONS, Tag.TAG_COMPOUND);
        for (var tag : reachedTag) {
            reachedPositions.add(NbtUtils.readBlockPos(HexUtils.downcast(tag, CompoundTag.TYPE)));
        }

        var currentPos = NbtUtils.readBlockPos(nbt.getCompound(TAG_CURRENT_POS));
        var enteredFrom = Direction.values()[nbt.getByte(TAG_ENTERED_FROM)];
        var image = CastingImage.loadFromNbt(nbt.getCompound(TAG_IMAGE), world);

        UUID caster = null;
        if (nbt.hasUUID(TAG_CASTER))
            caster = nbt.getUUID(TAG_CASTER);

        FrozenColorizer colorizer = FrozenColorizer.fromNBT(nbt.getCompound(TAG_COLORIZER));

        return new CircleExecutionState(startPos, startDir, knownPositions, reachedPositions, currentPos,
            enteredFrom, image, caster, colorizer);
    }

    /**
     * Update this, also mutates the impetus.
     * <p>
     * Returns whether to continue.
     */
    public boolean tick(BlockEntityAbstractImpetus impetus) {
        var world = (ServerLevel) impetus.getLevel();

        if (world == null)
            return true; // if the world is null, try again next tick.

        ServerPlayer caster = null;
        if (this.caster != null && world.getEntity(this.caster) instanceof ServerPlayer player)
            caster = player;

        var env = new CircleCastEnv(world, this);

        var executorBlockState = world.getBlockState(this.currentPos);
        if (!(executorBlockState.getBlock() instanceof ICircleComponent executor)) {
            // TODO: notification of the error?
            ICircleComponent.sfx(this.currentPos, executorBlockState, world, Objects.requireNonNull(env.getImpetus())
                , false);
            return false;
        }

        executorBlockState = executor.startEnergized(this.currentPos, executorBlockState, world);
        this.reachedPositions.add(this.currentPos);

        boolean halt = false;
        var ctrl = executor.acceptControlFlow(this.currentImage, env, this.enteredFrom, this.currentPos,
            executorBlockState, world);
        if (ctrl instanceof ICircleComponent.ControlFlow.Stop) {
            // acceptControlFlow should have already posted the error
            halt = true;
        } else if (ctrl instanceof ICircleComponent.ControlFlow.Continue cont) {
            Pair<BlockPos, Direction> found = null;

            for (var exit : cont.exits) {
                var there = world.getBlockState(exit.getFirst());
                if (there.getBlock() instanceof ICircleComponent cc
                    && cc.canEnterFromDirection(exit.getSecond(), exit.getFirst(), there, world)) {
                    if (found != null) {
                        // oh no!
                        impetus.postError(
                            Component.translatable("hexcasting.circles.many_exits",
                                Component.literal(this.currentPos.toShortString()).withStyle(ChatFormatting.RED)),
                            new ItemStack(Items.COMPASS));
                        ICircleComponent.sfx(this.currentPos, executorBlockState, world,
                            Objects.requireNonNull(env.getImpetus()), false);
                        halt = true;
                        break;
                    } else {
                        found = exit;
                    }
                }
            }

            if (found == null) {
                // will never enter here if there were too many because found will have been set
                impetus.postError(
                    Component.translatable("hexcasting.circles.no_exits",
                        Component.literal(this.currentPos.toShortString()).withStyle(ChatFormatting.RED)),
                    new ItemStack(Items.OAK_SIGN));
                ICircleComponent.sfx(this.currentPos, executorBlockState, world,
                    Objects.requireNonNull(env.getImpetus()), false);
                halt = true;
            } else {
                // A single valid exit position has been found.
                ICircleComponent.sfx(this.currentPos, executorBlockState, world,
                    Objects.requireNonNull(env.getImpetus()), true);
                currentPos = found.getFirst();
                enteredFrom = found.getSecond();
                currentImage = cont.update;
            }
        }

        return !halt;
    }

    /**
     * How many ticks should pass between activations, given the number of blocks encountered so far.
     */
    protected int getTickSpeed() {
        return Math.max(2, 10 - (this.reachedPositions.size() - 1) / 3);
    }

    public void endExecution(BlockEntityAbstractImpetus impetus) {
        var world = (ServerLevel) impetus.getLevel();

        if (world == null)
            return; // TODO: error here?

        for (var pos : this.reachedPositions) {
            var there = world.getBlockState(pos);
            if (there.getBlock() instanceof ICircleComponent cc) {
                cc.endEnergized(pos, there, world);
            }
        }
    }
}