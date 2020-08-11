package ftblag.lagbgonreborn;

import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;

public class LBGCommand extends CommandBase {

    private static LBGConfig config = LBGConfig.ins();
    private static long nextUnload;
    private static final String[] al = new String[]{"toggleitem", "toggleentity", "clear", "interval", "toggleauto", "listitems", "listentities", "settps", "unload", "blacklist", "togglepolice", "setbreedlimit", "scanentities", "maxperchunk"};
//    private static ArrayList<String> alias = new ArrayList<String>(Arrays.asList(new String[]{"toggleitem", "toggleentity", "clear", "interval", "toggleauto", "listitems", "listentities", "settps", "unload", "blacklist", "togglepolice", "setbreedlimit", "scanentities", "maxperchunk"}));

    @Override
    public String getName() {
        return "bgon";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bgon : 显示使用的帮助";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1)
            return getListOfStringsMatchingLastWord(args, al);
        else if (args.length == 2 && args[0].equals("toggleentity"))
            return getListOfStringsMatchingLastWord(args, EntityList.getEntityNameList());
        else
            return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            LagBGonReborn.sendMsg(sender, "/bgon toggleitem: 将手持物品加入黑名单。");
            LagBGonReborn.sendMsg(sender, "/bgon toggleentity <modid:name 生物名>: 将生物加入黑名单。");
            LagBGonReborn.sendMsg(sender, "/bgon clear: 清除黑名单中的物品和生物。");
            LagBGonReborn.sendMsg(sender, "/bgon interval <分钟>: 设置自动清理的间隔时间，请大于一分钟。");
            LagBGonReborn.sendMsg(sender, "/bgon toggleauto: 打开和关闭自动清理。");
            LagBGonReborn.sendMsg(sender, "/bgon listitems: 列出黑名单中的物品。");
            LagBGonReborn.sendMsg(sender, "/bgon listentities: 列出黑名单中的生物。");
            LagBGonReborn.sendMsg(sender, "/bgon settps <tps>: 设置低于多少TPS自动卸载区块。");
            LagBGonReborn.sendMsg(sender, "/bgon unload: 卸载未使用区块。");
            LagBGonReborn.sendMsg(sender, "/bgon blacklist: 切换黑名单和白名单模式。");
            LagBGonReborn.sendMsg(sender, "/bgon togglepolice: 启动生物监视器。");
            LagBGonReborn.sendMsg(sender, "/bgon setbreedlimit <数量>: 设置生物繁殖数量限制。");
            LagBGonReborn.sendMsg(sender, "/bgon scanentities: 列出附近生物名单。");
            LagBGonReborn.sendMsg(sender, "/bgon maxperchunk <数量>: 设置每个区块的最大生物数量。");
            LagBGonReborn.sendMsg(sender, "/bgon togglenamedremove: 切换名为“删除”。");
        } else if (args.length == 1) {
            if (args[0].equals("blacklist")) {
                config.toggleBlacklist();
                LagBGonReborn.sendMsg(sender, (LBGConfig.blacklist ? "白" : "黑") + "名单启用");
            } else if (args[0].equals("scanentities")) {
                if (!(sender instanceof EntityPlayer)) {
                    LagBGonReborn.sendMsg(sender, "只有玩家可以执行！");
                    return;
                }
                scanEntities((EntityPlayer) sender);
            } else if (args[0].equals("togglepolice")) {
                config.togglePolice();
                LagBGonReborn.sendMsg(sender, "生物监视器" + (LBGConfig.policeCrowd ? "启动" : "停止") + "。");
            } else if (args[0].equals("unload")) {
                unloadChunks();
            } else if (args[0].equals("listitems")) {
                StringBuilder line = new StringBuilder();
                LagBGonReborn.sendMsg(sender, "物品名列表：");
                for (String item : LBGConfig.itemBlacklist) {
                    if (line.length() > 40) {
                        LagBGonReborn.sendMsg(sender, line.toString());
                        line = new StringBuilder();
                    }
                    line.append(item);
                    line.append(", ");
                }
                if (line.length() > 0) {
                    LagBGonReborn.sendMsg(sender, (String) line.toString().subSequence(0, line.length() - 2));
                }
            } else if (args[0].equals("listentities")) {
                StringBuilder line = new StringBuilder();
                LagBGonReborn.sendMsg(sender, "生物名列表:");
                for (String item : LBGConfig.entityBlacklist) {
                    if (line.length() > 40) {
                        LagBGonReborn.sendMsg(sender, line.toString());
                        line = new StringBuilder();
                    }
                    line.append(item);
                    line.append(", ");
                }
                if (line.length() > 0) {
                    LagBGonReborn.sendMsg(sender, (String) line.toString().subSequence(0, line.length() - 2));
                }
            } else if (args[0].equals("toggleauto")) {
                config.toggleAuto();
                LagBGonReborn.sendMsg(sender, "自动清理 " + (LBGConfig.automaticRemoval ? "启用" : "停止") + "。");
            } else if (args[0].equals("toggleitem")) {
                if (!(sender instanceof EntityPlayer)) {
                    LagBGonReborn.sendMsg(sender, "只有玩家可以执行！");
                    return;
                }
                EntityPlayer plr = (EntityPlayer) sender;
                if (plr.getHeldItemMainhand().isEmpty()) {
                    LagBGonReborn.sendMsg(sender, "你必须手持一个物品。");
                    return;
                }
                Item item = plr.getHeldItemMainhand().getItem();
                config.toggleItem(item);
                boolean hav = !config.isBlacklisted(plr.getHeldItemMainhand().getItem());
                String nam = item.getItemStackDisplayName(plr.getHeldItemMainhand());
                LagBGonReborn.sendMsg(sender, nam + (hav ? " 从名单删除" : " 添加到名单") + "。");
            } else if (args[0].equals("clear")) {
                if (!DimensionManager.getWorld(0).isRemote) {
                    doClear();
                }
            } else if (args[0].equals("togglenamedremove")) {
                config.toggleNamedRemove();
                LagBGonReborn.sendMsg(sender, "命名为删除 " + (LBGConfig.namedRemove ? "启用" : "停止") + "。");
            } else {
                LagBGonReborn.sendMsg(sender, "命令没有找到！");
            }
        } else if (args.length == 2) {
            if (args[0].equals("maxperchunk")) {
                int max = Integer.parseInt(args[1]);
                config.changeMaxPerChunk(max);
                LagBGonReborn.sendMsg(sender, "每个块产生新的最大值" + max);
            } else if (args[0].equals("setbreedlimit")) {
                int limit = Integer.parseInt(args[1]);
                config.changeCrowdLimit(limit);
                LagBGonReborn.sendMsg(sender, "繁殖限制设置为： " + LBGConfig.crowdLimit);
            } else if (args[0].equals("toggleentity")) {
                config.toggleEntity(args[1]);
                boolean hav = config.isBlacklisted(args[1]);
                LagBGonReborn.sendMsg(sender, args[1] + " 已经" + (hav ? "添加到名单" : "从名单移除") + "。");
                LBGConfig.checkEntityBlacklist();
            } else if (args[0].equals("interval")) {
                int newInterval = Integer.parseInt(args[1]);
                config.changeInterval(newInterval);
                LBGEvents.nextClear = System.currentTimeMillis() + (LBGConfig.timeInterval * 1000 * 60);
                LagBGonReborn.sendMsg(sender, "自动清理间隔设置为： " + newInterval);
            } else if (args[0].equals("unload")) {
                int newInterval = Integer.parseInt(args[1]);
                config.changeUnload(newInterval);
                nextUnload = System.currentTimeMillis() + (LBGConfig.timeUnload * 1000 * 60);
                LagBGonReborn.sendMsg(sender, "自动清除卸载间隔设置为： " + newInterval);
            } else if (args[0].equals("settps")) {
                int newTPS = Integer.parseInt(args[1]);
                config.changeTPSForUnload(newTPS);
                LagBGonReborn.sendMsg(sender, "新的TPS最小值设置为:" + newTPS);
            } else {
                LagBGonReborn.sendMsg(sender, "命令没有找到！");
            }
        } else {
            if (args[0].equals("toggleentity")) {
                StringBuilder name = new StringBuilder();
                for (String word : args) {
                    if (!word.equals("toggleentity")) {
                        name.append(word);
                        name.append(" ");
                    }
                }
                name.replace(name.length() - 1, name.length(), "");
                config.toggleEntity(name.toString());
                boolean hav = config.isBlacklisted(name.toString());
                LagBGonReborn.sendMsg(sender, name.toString() + " 已经" + (hav ? "添加到名单" : "从名单移除") + "。");
            } else {
                LagBGonReborn.sendMsg(sender, "命令没有找到！");
            }
        }
    }

    public static void doClear() {
        EntityItem item;
        Entity entity;
        int itemsRemoved = 0;
        int entitiesRemoved = 0;
        ArrayList<Entity> toRemove = new ArrayList<>();
        for (World world : DimensionManager.getWorlds()) {
            if (world == null) {
                continue;
            }
            if (world.isRemote) {
                System.out.println("怎么样？！？");
            }
            Iterator<Entity> iter = world.loadedEntityList.iterator();
            Entity obj;
            while (iter.hasNext()) {
                obj = iter.next();
                if (obj instanceof EntityItem) {
                    item = (EntityItem) obj;
                    if (LBGConfig.blacklist && config.isBlacklisted(item.getItem().getItem())) {
                        toRemove.add(item);
                        itemsRemoved++;
                    }
                    if (!LBGConfig.blacklist && !config.isBlacklisted(item.getItem().getItem())) {
                        toRemove.add(item);
                        itemsRemoved++;
                    }
                } else if (!(obj instanceof EntityPlayer)) {
                    entity = obj;
                    if (config.isBlacklisted(entity) && LBGConfig.blacklist) {
                        toRemove.add(entity);
                    }
                    if (!config.isBlacklisted(entity) && !LBGConfig.blacklist) {
                        toRemove.add(entity);
                    }
                }
            }
            for (Entity e : toRemove) {
                if (e.hasCustomName() && LBGConfig.namedRemove || !e.hasCustomName() && !LBGConfig.namedRemove) {
                    e.setDead();
                    entitiesRemoved++;
                }
            }
            toRemove.clear();
        }
            FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(new TextComponentString("[减少卡顿] 以清理" + itemsRemoved + "个物品" + entitiesRemoved + "个生物。"));
    }

    @Override
    public int getRequiredPermissionLevel() {

        return 2;
    }

    private static long mean(long num[]) {
        long val = 0;
        for (long n : num) {
            val += n;
        }
        return val / num.length;
    }

    private static boolean unloadChunks() {

        ChunkProviderServer cPS;
        int oldChunksLoaded;
        int newChunksLoaded;
        boolean unloadSafe;

        oldChunksLoaded = 0;
        newChunksLoaded = 0;

        List<ChunkPos> playerPos = Lists.newArrayList();
        int radius = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getViewDistance() + 1;
        for (EntityPlayerMP player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
            for (int x = player.chunkCoordX - radius; x <= player.chunkCoordX + radius; x++) {
                for (int z = player.chunkCoordZ - radius; z <= player.chunkCoordZ + radius; z++) {
                    playerPos.add(new ChunkPos(x, z));
                }
            }
        }

        for (WorldServer world : DimensionManager.getWorlds()) {
            oldChunksLoaded += world.getChunkProvider().getLoadedChunkCount();
            if (world.getChunkProvider() instanceof ChunkProviderServer) {
                cPS = world.getChunkProvider();

                for (Chunk chunk : cPS.loadedChunks.values()) {
                    ChunkPos chunkPos = new ChunkPos(chunk.x, chunk.z);
                    unloadSafe = !world.getPersistentChunks().containsKey(chunkPos);
                    if (unloadSafe) {
                        unloadSafe = !playerPos.contains(chunkPos);
//                        for (EntityPlayerMP player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
//                            if ((player.chunkCoordX == chunk.x && player.chunkCoordZ == chunk.z)) {
//                                unloadSafe = false;
//                                break;
//                            }
//                        }
                    }
                    if (unloadSafe) {
                        cPS.queueUnload(chunk);
                    }

                }
                cPS.tick();

            }
            newChunksLoaded += world.getChunkProvider().getLoadedChunkCount();
        }
        nextUnload = System.currentTimeMillis() + (LBGConfig.timeUnload * 1000 * 60);
        FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(new TextComponentString((oldChunksLoaded - newChunksLoaded) + " chunks unloaded by Lag'B'Gon."));

        return true;

    }

    public static boolean checkTPS() {
        double meanTickTime = mean(FMLCommonHandler.instance().getMinecraftServerInstance().tickTimeArray) * 1.0E-6D;
        double meanTPS = Math.min(1000.0 / meanTickTime, 20);
        if (nextUnload < System.currentTimeMillis()) {
            if (meanTPS < LBGConfig.TPSForUnload) {
                unloadChunks();
                return true;
            }
        }
        return false;
    }

    private void scanEntities(EntityPlayer plr) {
        List<Entity> entities = plr.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(plr.getPosition()).grow(5));
        if (entities.isEmpty())
            return;
        ArrayList<String> entityNames = new ArrayList<>();
        for (Entity ent : entities) {
            if (!entityNames.contains(ent instanceof EntityPlayer ? ent.getName() : EntityList.getKey(ent).toString())) {
                entityNames.add(ent instanceof EntityPlayer ? ent.getName() : EntityList.getKey(ent).toString());
            }
        }

        StringBuilder line = new StringBuilder();
        LagBGonReborn.sendMsg(plr, "附近的生物");
        for (String item : entityNames) {
            if (line.length() > 40) {
                LagBGonReborn.sendMsg(plr, line.toString());
                line = new StringBuilder();
            }
            line.append(item);
            line.append(", ");
        }
        if (line.length() > 0) {
            LagBGonReborn.sendMsg(plr, (String) line.toString().subSequence(0, line.length() - 2));
        }
    }
}
