package ftblag.lagbgonreborn;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LBGConfig {
    public static Configuration cfg;

    public static List<String> entityBlacklist = new ArrayList<>(), itemBlacklist = new ArrayList<>();
//    public static ArrayList<Item> itemsBlackList = new ArrayList<>();
    public static int timeInterval, TPSForUnload, crowdLimit, perChunkSpawnLimit, timeUnload;
    public static boolean automaticRemoval, policeCrowd, blacklist, namedRemove, redBoldWarning;

    private static LBGConfig ins;

    public static LBGConfig ins() {
        if (ins == null) {
            ins = new LBGConfig();
        }
        return ins;
    }

    public void init(Configuration cfg) {
        LBGConfig.cfg = cfg;
    }

    public void load() {
        cfg.load();
        entityBlacklist = new ArrayList<>(Arrays.asList(cfg.get(Configuration.CATEGORY_GENERAL, "EntityBlackList", new String[]{"minecraft:cow",}, "不能销毁的动物列表。").getStringList()));
        itemBlacklist = new ArrayList<>(Arrays.asList(cfg.get(Configuration.CATEGORY_GENERAL, "ItemBlackList", new String[]{"minecraft:diamond",}, "不能销毁的物品清单。").getStringList()));
        timeInterval = cfg.get(Configuration.CATEGORY_GENERAL, "Interval", 15, "清除动物之间的间隔(以分钟为单位)。间隔实际上长了1分钟，因为它包含了一个1分钟的警告。").getInt();
        automaticRemoval = cfg.get(Configuration.CATEGORY_GENERAL, "AutomaticRemoval", true).getBoolean();
        TPSForUnload = cfg.get(Configuration.CATEGORY_GENERAL, "TPSForUnload", 12, "如果服务器的主TPS低于这个数字，\n<减少卡顿>将尝试卸载块来改进TPS").getInt();
        crowdLimit = cfg.get("Breeding", "CrowdLimit", 10).getInt();
        policeCrowd = cfg.get("Breeding", "PoliceCrowding", false, "如果服务器的主TPS低于这个数字，\n<减少卡顿>将尝试卸载块来改善TPS防止过度繁殖。如果在5个区块内有可繁殖的动物，那么动物将不会繁殖。\n将此值设置为小于3可以完全防止繁殖。").getBoolean();
        perChunkSpawnLimit = cfg.get(Configuration.CATEGORY_GENERAL, "PerChunkSpawnLimit", 0, "每块可生成的最大群。0禁用。").getInt();
        blacklist = cfg.get(Configuration.CATEGORY_GENERAL, "Blacklist", true, "我们应该使用黑名单或白名单来清除怪物吗?\n默认为黑名单。").getBoolean();
        namedRemove = cfg.get(Configuration.CATEGORY_GENERAL, "NamedRemove", false, "删除命名动物?(名牌)").getBoolean();
        timeUnload = cfg.get(Configuration.CATEGORY_GENERAL, "Unload", 15, "卸载块之间的间隔(以分钟为单位)。").getInt();
        redBoldWarning = cfg.get(Configuration.CATEGORY_GENERAL, "RedBoldWarning", false, "为警告信息添加红色粗体样式").getBoolean();
        cfg.save();
//        updateBlacklist();
        checkEntityBlacklist();
    }

    public static void checkEntityBlacklist() {
        for (String str : entityBlacklist) {
            if (!str.contains("*") && !ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(str))) {
                System.out.println("[LagBGonReborn] 发现错误 mob id! ID: " + str);
            }
        }
    }

//    private void updateBlacklist() {
//        itemsBlackList.clear();
//        for (String str : itemBlacklist) {
//            itemsBlackList.add(Item.REGISTRY.getObject(new ResourceLocation(str)));
//        }
//    }

    public void toggleAuto() {
        automaticRemoval = !automaticRemoval;
        save();
    }

    public void toggleBlacklist() {
        blacklist = !blacklist;
        save();
    }

    public void toggleNamedRemove() {
        namedRemove = !namedRemove;
        save();
    }

    public void changeMaxPerChunk(int newMax) {
        if (newMax < 0) {
            newMax = 0;
        }
        perChunkSpawnLimit = newMax;
        save();
    }

    public void changeCrowdLimit(int newLimit) {
        if (newLimit < 1) {
            newLimit = 1;
        }
        crowdLimit = newLimit;
        save();
    }

    public void changeInterval(int newInterval) {
        if (newInterval < 1) {
            newInterval = 1;
        }
        timeInterval = newInterval;
        save();
    }

    public void changeUnload(int newInterval) {
        if (newInterval < 1) {
            newInterval = 1;
        }
        timeUnload = newInterval;
        save();
    }

    public void changeTPSForUnload(int newTPS) {
        if (newTPS > 15) {
            TPSForUnload = 15;
        } else {
            TPSForUnload = newTPS;
        }
        save();
    }

    public void togglePolice() {
        policeCrowd = !policeCrowd;
        save();
    }

    public void toggleItem(Item item) {
//        if (itemsBlackList.contains(item)) {
//            itemBlacklist.remove(Item.REGISTRY.getNameForObject(item).toString());
//            itemsBlackList.remove(item);
//        } else {
//            itemBlacklist.add(Item.REGISTRY.getNameForObject(item).toString());
//            itemsBlackList.add(item);
//        }
        String name = item.getRegistryName().toString();
        if (itemBlacklist.contains(name)) {
//            itemBlacklist.remove(Item.REGISTRY.getNameForObject(item).toString());
            itemBlacklist.remove(name);
//            itemsBlackList.remove(item);
        } else {
//            itemBlacklist.add(Item.REGISTRY.getNameForObject(item).toString());
            itemBlacklist.add(name);
//            itemsBlackList.add(item);
        }
        save();
    }

    public void toggleEntity(String name) {
        if (entityBlacklist.contains(name)) {
            entityBlacklist.remove(name);
        } else {
            entityBlacklist.add(name);
        }
        save();
    }

    public boolean isBlacklisted(Item item) {
        ResourceLocation name = item.getRegistryName();
        return itemBlacklist.contains(name.toString()) || itemBlacklist.contains(name.getNamespace() + ":*");
    }

    public boolean isBlacklisted(Entity entity) {
        if (entity == null)
            return false;

        ResourceLocation rl = EntityList.getKey(entity);
        if (rl != null) {
            return entityBlacklist.contains(rl.toString()) || entityBlacklist.contains(rl.getNamespace() + ":*");
        } else {
            String className = entity.getClass().toString();
            System.out.println("未能获得注册的怪物名字! Class: " + className);
            return entityBlacklist.contains(className);
        }
    }

    public boolean isBlacklisted(String name) {
        return entityBlacklist.contains(name) || itemBlacklist.contains(name);
    }

    public static void save() {
        cfg.get(Configuration.CATEGORY_GENERAL, "EntityBlackList", new String[]{"minecraft:cow",}, "不能销毁的动物列表。").set(entityBlacklist.toArray(new String[entityBlacklist.size()]));
        cfg.get(Configuration.CATEGORY_GENERAL, "ItemBlackList", new String[]{"minecraft:diamond",}, "不能销毁的物品清单和不能销毁的物品清单。").set(itemBlacklist.toArray(new String[itemBlacklist.size()]));
        cfg.get(Configuration.CATEGORY_GENERAL, "Interval", 15, "清除动物之间的间隔(以分钟为单位)。").set(timeInterval);
        cfg.get(Configuration.CATEGORY_GENERAL, "AutomaticRemoval", true).set(automaticRemoval);
        cfg.get(Configuration.CATEGORY_GENERAL, "TPSForUnload", 12, "如果服务器的主TPS低于这个数字，\n<减少卡顿>将尝试卸载块来改进TPS").set(TPSForUnload);
        cfg.get("Breeding", "CrowdLimit", 10).set(crowdLimit);
        cfg.get("Breeding", "PoliceCrowding", false, "防止overbreeding。如果在5个区块内有可繁殖的动物，那么动物将不会繁殖。\n将此值设置为小于3可以完全防止繁殖。").set(policeCrowd);
        cfg.get(Configuration.CATEGORY_GENERAL, "PerChunkSpawnLimit", 0, "每块可生成的最大群。0禁用。").set(perChunkSpawnLimit);
        cfg.get(Configuration.CATEGORY_GENERAL, "Blacklist", true, "我们应该使用黑名单或白名单来清除怪物吗?\n默认为黑名单。").set(blacklist);
        cfg.get(Configuration.CATEGORY_GENERAL, "NamedRemove", false, "删除命名动物?(名牌)").set(namedRemove);
        cfg.get(Configuration.CATEGORY_GENERAL, "Unload", 15, "卸载块之间的间隔(以分钟为单位)。").set(timeUnload);
        cfg.save();
    }
}
