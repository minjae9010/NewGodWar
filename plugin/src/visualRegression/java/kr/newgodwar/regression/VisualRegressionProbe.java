package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.builtin.AbilityDesigns;
import kr.newgodwar.ability.builtin.BaseAbility;
import kr.newgodwar.ability.feedback.*;
import kr.newgodwar.game.GodTeam;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Disposable local visual fixture; never included in the production plugin. */
public final class VisualRegressionProbe extends JavaPlugin {
    private NewGodWarPlugin core;
    private final Map<String, ObjectModel> models = new TreeMap<String, ObjectModel>();
    private final Map<String, String> descriptions = new TreeMap<String, String>();
    private final IdentityHashMap<Object, String> modelNames = new IdentityHashMap<Object, String>();
    private AbilityFeedback feedback;
    private int outlineTask = -1;
    private long sequence;
    private Player target;
    private String current;

    @Override public void onEnable() {
        if (getServer().getPort() != 25576 || !"127.0.0.1".equals(getServer().getIp())) {
            throw new IllegalStateException("Visual probe is restricted to the isolated loopback test server");
        }
        core = (NewGodWarPlugin) getServer().getPluginManager().getPlugin("NewGodWar");
        try {
            collect("design", AbilityDesigns.class);
            collect("shared", SharedModels.class);
            for (String id : core.abilities().registry().ids()) collect(id, core.abilities().registry().get(id).create().getClass());
        } catch (Exception error) { throw new IllegalStateException(error); }
        getCommand("ngwvisual").setExecutor(this);
    }

    private void collect(String prefix, Class<?> type) throws Exception {
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !(field.getType() == ObjectModel.class || field.getType() == DesignedEffect.class)) continue;
            field.setAccessible(true);
            Object value = field.get(null);
            String key = prefix + "." + field.getName();
            models.put(key, value instanceof DesignedEffect ? ((DesignedEffect)value).model(true) : (ObjectModel)value);
            descriptions.put(key, value instanceof DesignedEffect ? ((DesignedEffect)value).description() : key);
            modelNames.put(value, key);
        }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.getName().equals("NGWVisualTest") || !sender.isOp()) return false;
        Map<String,Object> result = map("sequence", ++sequence, "command", Arrays.asList(args));
        try {
            Player player = (Player)sender;
            switch (args[0]) {
                case "catalog": result.put("catalog", catalog()); break;
                case "prepare": prepare(player, args[1], args.length > 2 ? args[2] : "enemy"); result.putAll(state(player)); break;
                case "state": result.putAll(state(player)); break;
                case "pack": core.effectArtPack().offer(player); result.putAll(state(player)); break;
                case "cue": cue(player, args[1], args[2], args[3]); result.putAll(state(player)); break;
                case "model": model(player, args[1], args[2]); result.putAll(state(player)); break;
                case "passive": result.put("observations",passive(player)); result.putAll(state(player)); break;
                case "condition": condition(player,args[1]); result.putAll(state(player)); break;
                case "foods": result.put("foods",foods(player,args.length>1?args[1]:"gallery")); result.putAll(state(player)); break;
                case "chat": core.abilities().handleChatMessage(player, String.join(" ", Arrays.copyOfRange(args,1,args.length))); result.putAll(state(player)); break;
                case "clear": clear(); core.game().stop(false); result.putAll(state(player)); break;
                default: throw new IllegalArgumentException("Unknown fixture command");
            }
            result.put("ok", true);
        } catch (Throwable error) {
            result.put("ok", false); result.put("error", error.toString());
            getLogger().log(java.util.logging.Level.SEVERE, "Visual fixture failed", error);
        }
        try {
            Files.createDirectories(getDataFolder().toPath());
            Path temporary = getDataFolder().toPath().resolve("last-result.tmp");
            Files.write(temporary, json(result).getBytes(StandardCharsets.UTF_8));
            Files.move(temporary, getDataFolder().toPath().resolve("last-result.json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception error) { throw new IllegalStateException(error); }
        return true;
    }

    private Object catalog() {
        List<Object> abilities = new ArrayList<Object>(), visualModels = new ArrayList<Object>();
        for (String id : new TreeSet<String>(core.abilities().registry().ids())) {
            AbilityDefinition definition = core.abilities().registry().get(id);
            GodAbility ability = definition.create(); AbilityStyle style = ability.style();
            Map<String,Object> effects = new LinkedHashMap<String,Object>();
            for (Map.Entry<EffectCue,DesignedEffect> entry : style.effects().entrySet()) effects.put(entry.getKey().name(), modelNames.get(entry.getValue()));
            Set<String> cues = new TreeSet<String>(effects.keySet());
            for (EffectCue cue : Arrays.asList(style.cast(false),style.cast(true),style.hit(),style.benefit(),style.passive())) if (cue != EffectCue.NONE) cues.add(cue.name());
            abilities.add(map("id",id,"name",definition.name(),"normal",definition.normalSkill(),"advanced",definition.advancedSkill(),
                "passive",definition.passiveSkill(),"effects",effects,"cues",cues,"flight",modelNames.get(style.flightModel()),"dedicated",style.dedicated()));
        }
        for (Map.Entry<String,ObjectModel> entry : models.entrySet()) visualModels.add(map("id",entry.getKey(),"description",descriptions.get(entry.getKey()),"parts",entry.getValue().parts(8,1).size(),"artParts",ArtModels.variant(entry.getValue()).parts(8,1).size()));
        return map("abilities",abilities,"models",visualModels);
    }

    private Object foods(Player player,String selection) throws Exception {
        if (!"siksin".equals(current)) throw new IllegalStateException("Prepare siksin first");
        GodAbility ability=core.abilities().session(player).ability();
        Class<?> kind=null;
        for(Class<?> nested:ability.getClass().getDeclaredClasses()) if(nested.getSimpleName().equals("BuffKind")) kind=nested;
        if(kind==null) throw new IllegalStateException("Missing food catalogue");
        Method create=ability.getClass().getDeclaredMethod("createFood",Player.class,boolean.class,kind);create.setAccessible(true);
        org.bukkit.inventory.Inventory gallery=Bukkit.createInventory(null,27,"식신 · 여섯 성찬");
        List<Object> details=new ArrayList<Object>();int i=0;
        player.closeInventory();
        for(Object buff:kind.getEnumConstants()) {
            ItemStack food=(ItemStack)create.invoke(ability,player,true,buff);
            gallery.setItem(10+i,food);gallery.setItem(19+i,new ItemStack(food.getType()));
            details.add(map("name",food.getItemMeta().getDisplayName(),"material",food.getType().name(),"lore",food.getItemMeta().getLore()));
            if(selection.equals(Integer.toString(i))) {player.getInventory().setHeldItemSlot(0);player.setItemInHand(food);player.setFoodLevel(12);}
            i++;
        }
        if(selection.equals("gallery")) player.openInventory(gallery);
        return details;
    }

    private void clear() {
        if (outlineTask >= 0) getServer().getScheduler().cancelTask(outlineTask);
        outlineTask = -1;
        if (feedback != null) feedback.clear();
        feedback = null;
    }

    private void prepare(Player player, String id, String role) throws Exception {
        clear(); core.game().stop(false);
        if (player.isDead()) player.spigot().respawn();
        World world = player.getWorld();
        for (Entity entity : new ArrayList<Entity>(world.getEntities())) if (!(entity instanceof Player)) entity.remove();
        for(int x=-10;x<=10;x++) for(int z=-10;z<=10;z++) {
            world.getBlockAt(x,-61,z).setType(Material.GRASS);
            for(int y=-60;y<=-53;y++) world.getBlockAt(x,y,z).setType(Material.AIR);
        }
        world.setTime(6000); world.setStorm(false);
        player.setGameMode(GameMode.SURVIVAL); player.setFlying(false); player.setFireTicks(0); player.setFallDistance(0);
        for(org.bukkit.potion.PotionEffect effect:player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        player.setMaxHealth(20); player.setFoodLevel(20); player.setSaturation(0);
        player.teleport(new Location(world,0.5,-60,0.5,0,0)); player.setVelocity(new Vector());
        player.getInventory().clear(); player.getInventory().setHeldItemSlot(0);
        core.game().startTest(player, core.abilities().registry().get(id)); current=id;
        player.getInventory().setItem(0,new ItemStack(Material.BLAZE_ROD));
        for(int i=1;i<10;i++) player.getInventory().setItem(i,new ItemStack(Material.COBBLESTONE,64));
        player.getInventory().setItem(10,new ItemStack(Material.IRON_INGOT,64));
        player.getInventory().setItem(11,new ItemStack(Material.IRON_SWORD));
        player.getInventory().setItem(12,new ItemStack(Material.ARROW,64));
        target = core.trainingDummies().spawn(player);
        target.teleport(player.getLocation().clone().add(0,0,4));
        core.game().assign(target, role.equals("ally") ? core.game().teamOf(player) : GodTeam.BLUE);
        core.abilities().set(target,core.abilities().registry().get("anorexia"));
        target.getInventory().addItem(new ItemStack(Material.COBBLESTONE,64));
        GodAbility ability = core.abilities().session(player).ability();
        if (ability.requiresTarget()) core.abilities().setTarget(player, player, target.getName());
        player.setHealth(12); target.setHealth(16);
    }

    private void cue(Player player, String id, String cue, String mode) throws Exception {
        clear();
        GodAbility ability = core.abilities().registry().get(id).create();
        Field field = BaseAbility.class.getDeclaredField("feedback"); field.setAccessible(true);
        feedback = (AbilityFeedback)field.get(ability);
        core.getConfig().set("abilities.effects.objects", !mode.equals("particles"));
        core.getConfig().set("abilities.effects.particles", true);
        AbilityPlayerContext context = new AbilityPlayerContext(core,player,core.abilities().registry().get(id));
        feedback.drawCue(context,player.getLocation(),EffectCue.valueOf(cue),Collections.singletonList(player),player,true);
    }

    private void model(Player player, String name, String mode) {
        clear();
        ObjectModel model = models.get(name);
        if (model == null) throw new IllegalArgumentException("Unknown model " + name);
        feedback = new AbilityFeedback();
        AbilityPlayerContext context = new AbilityPlayerContext(core,player,core.abilities().registry().get("invincibility"));
        Location anchor = AbilityFeedback.upright(player.getLocation());
        if(name.equals("anubis.SCALES")) anchor.add(0,2.6,0);
        if(name.equals("thor.HAMMER") || name.equals("shared.SPEAR")) anchor.add(0,1.4,0);
        if(name.equals("graviton.GRAVITY")) anchor.add(0,1.5,0);
        core.getConfig().set("abilities.effects.objects", !mode.equals("particles"));
        core.getConfig().set("abilities.effects.particles", true);
        if (!mode.equals("particles")) {
            if (!feedback.object(context,"fixture",model,anchor,18,1)) throw new IllegalStateException("Model used an unexpected fallback");
        } else {
            final int[] age={0};
            outlineTask=getServer().getScheduler().scheduleSyncRepeatingTask(this,()->{
                feedback.modelOutline(context,anchor,model,age[0],1,Collections.singletonList(player));
                age[0]+=4;
                if(age[0]>=18) { getServer().getScheduler().cancelTask(outlineTask); outlineTask=-1; }
            },0,4);
        }
    }

    private void condition(Player player,String kind) throws Exception {
        GodAbility ability=core.abilities().session(player).ability();
        AbilityPlayerContext context=new AbilityPlayerContext(core,player,core.abilities().get(player));
        if(kind.equals("normal") && current.equals("nasdaq")) {
            player.setItemInHand(new ItemStack(Material.IRON_INGOT,8));
            core.getConfig().set("abilities.nasdaq.iron-success-percent",100);
        }
        if(kind.equals("normal") && current.equals("sniper")) {
            player.setItemInHand(new ItemStack(Material.BOW));player.setSneaking(true);
            core.abilities().handleInteract(player,new PlayerInteractEvent(player,Action.LEFT_CLICK_AIR,player.getItemInHand(),null,org.bukkit.block.BlockFace.SELF));
        }
        if(kind.equals("advanced") && current.equals("sniper")) {
            Arrow arrow=player.launchProjectile(Arrow.class);
            if(arrow.getVelocity().length()<19) throw new IllegalStateException("Sniper arrow was not accelerated");
        }
        if(kind.equals("advanced") && current.equals("echo")) {
            player.setItemInHand(new ItemStack(Material.IRON_SWORD));
            core.abilities().handleDamage(player,target,new EntityDamageByEntityEvent(player,target,EntityDamageEvent.DamageCause.ENTITY_ATTACK,4));
            player.setItemInHand(new ItemStack(Material.BLAZE_ROD));
        }
        if(kind.equals("advanced") && current.equals("nike")) {
            core.abilities().handleKill(player,target,deathEvent(target));
        }
        if(kind.equals("advanced") && current.equals("anubis"))
            core.abilities().handleDamage(target,player,new EntityDamageByEntityEvent(target,player,EntityDamageEvent.DamageCause.ENTITY_ATTACK,4));
        if(kind.equals("advanced") && current.equals("runesmith")) target.teleport(player.getLocation().clone().add(0,0,2));
        if(kind.equals("advanced") && current.equals("thor")) target.teleport(player.getLocation().clone().add(0,0,2));
        if(kind.equals("normal") && current.equals("voodoo")) {
            org.bukkit.block.Block block=player.getLocation().clone().add(1,0,1).getBlock();block.setType(Material.SIGN_POST);
            core.abilities().handleSignChange(player,new SignChangeEvent(block,player,new String[]{target.getName(),"","",""}));
            core.abilities().handleInteract(player,new PlayerInteractEvent(player,Action.LEFT_CLICK_BLOCK,player.getItemInHand(),block,org.bukkit.block.BlockFace.UP));
        }
    }

    private Map<String,Object> passive(Player player) throws Exception {
        GodAbility ability=core.abilities().session(player).ability();
        AbilityPlayerContext context=new AbilityPlayerContext(core,player,core.abilities().get(player));
        Map<String,Object> observed=new LinkedHashMap<String,Object>();
        Field random=BaseAbility.class.getDeclaredField("RANDOM");random.setAccessible(true);((Random)random.get(null)).setSeed(123456L);
        if(current.equals("miner")||current.equals("jangyeongsil"))player.setItemInHand(new ItemStack(Material.IRON_PICKAXE));
        if(current.equals("thor"))player.setItemInHand(new ItemStack(Material.IRON_AXE));
        if(current.equals("sejong"))player.setItemInHand(new ItemStack(Material.BOOK));
        if(current.equals("anjunggeun"))player.setItemInHand(new ItemStack(Material.IRON_SWORD));
        if(current.equals("midoriya") || current.equals("onepunch") || current.equals("tajja")) player.setItemInHand(new ItemStack(Material.AIR));
        EntityDamageByEntityEvent hit=new EntityDamageByEntityEvent(player,target,EntityDamageEvent.DamageCause.ENTITY_ATTACK,4);
        core.abilities().handleDamage(player,target,hit);
        observed.put("outgoingDamage",hit.getDamage());
        EntityDamageByEntityEvent incoming=new EntityDamageByEntityEvent(target,player,EntityDamageEvent.DamageCause.ENTITY_ATTACK,3);
        core.abilities().handleDamage(target,player,incoming);
        observed.put("incomingDamage",incoming.getDamage());observed.put("incomingCancelled",incoming.isCancelled());
        if(Arrays.asList("dionysus","eris","reflection","ares","blinder","honggildong","loki","rickroll","shinsaimdang","sus","witch").contains(current)) for(int i=0;i<40;i++) {
            ability.onDamageByEntity(context,new EntityDamageByEntityEvent(target,player,EntityDamageEvent.DamageCause.ENTITY_ATTACK,1),target,false);
        }
        Map<String,Object> damageChecks=new LinkedHashMap<String,Object>();
        for(EntityDamageEvent.DamageCause cause:Arrays.asList(EntityDamageEvent.DamageCause.FIRE_TICK,EntityDamageEvent.DamageCause.DROWNING,
                EntityDamageEvent.DamageCause.FALL,EntityDamageEvent.DamageCause.LIGHTNING,EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
                EntityDamageEvent.DamageCause.POISON,EntityDamageEvent.DamageCause.WITHER)) {
            EntityDamageEvent damage=new EntityDamageEvent(player,cause,4);
            ability.onGenericDamage(context,damage);
            damageChecks.put(cause.name(),map("damage",damage.getDamage(),"cancelled",damage.isCancelled()));
            if(cause==EntityDamageEvent.DamageCause.FIRE_TICK && Arrays.asList("amaterasu","hephaestus","jujak","ra","thisisfine").contains(current))require(damage.isCancelled(),"Fire immunity missing");
            if(cause==EntityDamageEvent.DamageCause.DROWNING && current.equals("poseidon"))require(damage.isCancelled(),"Drowning immunity missing");
            if(cause==EntityDamageEvent.DamageCause.DROWNING && Arrays.asList("jujak","hephaestus").contains(current))require(damage.getDamage()==8,"Drowning multiplier missing");
            if(cause==EntityDamageEvent.DamageCause.FALL && Arrays.asList("naro","quetzalcoatl").contains(current))require(damage.isCancelled(),"Fall immunity missing");
            if(cause==EntityDamageEvent.DamageCause.LIGHTNING && Arrays.asList("thor","zeus").contains(current))require(damage.isCancelled(),"Lightning immunity missing");
            if((cause==EntityDamageEvent.DamageCause.POISON||cause==EntityDamageEvent.DamageCause.WITHER)&&current.equals("heojun"))require(damage.isCancelled(),"Poison immunity missing");
        }
        observed.put("damageChecks",damageChecks);
        if(current.equals("creeper")) {
            Field plasma=ability.getClass().getDeclaredField("plasma");plasma.setAccessible(true);
            boolean charged=plasma.getBoolean(ability);require(charged,"Creeper lightning charge missing");observed.put("lightningCharged",charged);
        }
        if(Arrays.asList("acidarcher","archer","artemis","sniper","snow").contains(current)) {
            Projectile projectile=current.equals("snow")?player.launchProjectile(Snowball.class):player.launchProjectile(Arrow.class);
            EntityDamageByEntityEvent projectileHit=new EntityDamageByEntityEvent(projectile,target,EntityDamageEvent.DamageCause.PROJECTILE,4);
            core.abilities().handleProjectileHit(player,target,projectileHit);
            observed.put("projectileDamage",projectileHit.getDamage());observed.put("projectileCancelled",projectileHit.isCancelled());
            if(current.equals("acidarcher"))require(projectileHit.getDamage()==0&&target.hasPotionEffect(org.bukkit.potion.PotionEffectType.POISON),"Poison arrow missing");
            if(current.equals("archer"))require(projectileHit.getDamage()>4,"Archer bonus missing");
            if(current.equals("snow"))require(projectileHit.isCancelled(),"Snowball handler missing");
            projectile.remove();
        }
        if(current.equals("hades")) {
            boolean preserved=false;
            for(int i=0;i<40;i++) {PlayerDeathEvent death=deathEvent(player);death.getDrops().add(new ItemStack(Material.STONE));ability.onDeath(context,death);if(death.getDrops().isEmpty())preserved=true;}
            require(preserved,"Hades inventory preservation never triggered");
            ability.onRespawn(context,new PlayerRespawnEvent(player,player.getLocation(),false));observed.put("inventoryPreserved",true);
        }
        ability.onGenericDamage(context,new EntityDamageEvent(player,EntityDamageEvent.DamageCause.FIRE_TICK,1));
        if(current.equals("zet"))for(int i=0;i<20;i++) ability.onGenericDamage(context,new EntityDamageEvent(player,EntityDamageEvent.DamageCause.FIRE_TICK,1));
        FoodLevelChangeEvent food=new FoodLevelChangeEvent(player,4);ability.onFoodLevelChange(context,food);observed.put("food",food.getFoodLevel());
        ability.onTick(context); ability.onCountdownTick(context);
        if(current.equals("bulter")) {
            org.bukkit.block.Block block=player.getLocation().getBlock();
            BlockExplodeEvent explosion=null;
            for(Constructor<?> constructor:BlockExplodeEvent.class.getConstructors()) {
                Class<?>[] types=constructor.getParameterTypes();
                if(types.length==3)explosion=(BlockExplodeEvent)constructor.newInstance(block,new ArrayList<org.bukkit.block.Block>(),1F);
                if(types.length==5)explosion=(BlockExplodeEvent)constructor.newInstance(block,block.getState(),new ArrayList<org.bukkit.block.Block>(),1F,types[4].getEnumConstants()[0]);
            }
            require(explosion!=null,"Explosion event constructor missing");
            ability.onBlockExplode(context,explosion);observed.put("explosionCancelled",explosion.isCancelled());
            require(explosion.isCancelled(),"Butler did not cancel the explosion");
        }
        if(current.equals("gardener")) {
            org.bukkit.block.Block block=player.getLocation().clone().add(1,0,1).getBlock();block.setType(Material.LOG);
            ability.onBlockBreak(context,new BlockBreakEvent(block,player));block.setType(Material.AIR);
            observed.put("droppedRewards",player.getWorld().getEntitiesByClass(Item.class).size());
            require(player.getWorld().getEntitiesByClass(Item.class).size()>=2,"Gardener rewards missing");
        }
        if(current.equals("fisher")) {
            Item caught=player.getWorld().dropItemNaturally(player.getLocation(),new ItemStack(Material.RAW_FISH));
            FishHook hook=player.launchProjectile(FishHook.class);
            PlayerFishEvent fishEvent=null;
            for(Constructor<?> constructor:PlayerFishEvent.class.getConstructors())if(constructor.getParameterTypes().length==4)
                fishEvent=(PlayerFishEvent)constructor.newInstance(player,caught,hook,PlayerFishEvent.State.CAUGHT_FISH);
            require(fishEvent!=null,"Fishing event constructor missing");
            ability.onFish(context,fishEvent);hook.remove();
            require(!caught.isValid(),"Fisher did not replace the caught fish");observed.put("caughtReplaced",true);
        }
        if(current.equals("goldspoon")) {
            ability.onRespawn(context,new PlayerRespawnEvent(player,player.getLocation(),false));
            boolean found=false;for(ItemStack item:player.getInventory().getContents())if(item!=null&&item.getType().name().endsWith("LEGGINGS"))found=true;
            require(found,"Goldspoon leggings missing");observed.put("leggings",found);
        }
        if(current.equals("pokego")) {
            for(int i=0;i<1000;i++)ability.onMove(context,new PlayerMoveEvent(player,player.getLocation(),player.getLocation().clone().add(0.2,0,0)));
            observed.put("newAbility",core.abilities().get(player).id());require(!core.abilities().get(player).id().equals("pokego"),"Pokego did not change ability");
        }
        if(current.equals("scrooge")) {
            int cost=core.abilities().effectiveResourceCost(player,10);observed.put("discountedCost",cost);require(cost==5,"Scrooge discount missing");
        }
        if(current.equals("examinee")) {
            Field answer=ability.getClass().getDeclaredField("pendingAnswer");answer.setAccessible(true);
            int value=answer.getInt(ability);require(value>=0,"Examinee question missing");
            core.abilities().handleChatMessage(player,String.valueOf(value));observed.put("newAbility",core.abilities().get(player).id());
        }
        if(current.equals("siksin")) {
            for(ItemStack item:player.getInventory().getContents())if(item!=null&&item.hasItemMeta()&&item.getItemMeta().hasLore()) {
                ability.onItemConsume(context,new PlayerItemConsumeEvent(player,item));observed.put("foodConsumed",true);break;
            }
        }
        if(current.equals("anorexia"))require(food.getFoodLevel()==10,"Anorexia hunger incorrect");
        if(current.equals("ares"))require(Math.abs(hit.getDamage()-5.6)<0.01,"Ares multiplier incorrect");
        if(current.equals("darkness"))require(hit.getDamage()==0&&Math.abs(incoming.getDamage()-0.75)<0.01,"Darkness multipliers incorrect");
        if(current.equals("stance"))require(incoming.isCancelled(),"Stance failed to intercept damage");
        if(current.equals("miner"))require(hit.getDamage()==4,"Miner pickaxe damage incorrect");
        observed.put("playerPotionCount",player.getActivePotionEffects().size());observed.put("targetPotionCount",target.getActivePotionEffects().size());
        return observed;
    }

    private static void require(boolean condition,String message) { if(!condition)throw new IllegalStateException(message); }

    private PlayerDeathEvent deathEvent(Player player) throws Exception {
        for(Constructor<?> constructor:PlayerDeathEvent.class.getConstructors()) {
            Class<?>[] types=constructor.getParameterTypes();
            if(types.length==4) return (PlayerDeathEvent)constructor.newInstance(player,new ArrayList<ItemStack>(),0,null);
            if(types.length==5 && types[4]==String.class) {
                EntityDamageEvent damage=new EntityDamageEvent(player,EntityDamageEvent.DamageCause.CUSTOM,1);
                Object source=damage.getClass().getMethod("getDamageSource").invoke(damage);
                return (PlayerDeathEvent)constructor.newInstance(player,source,new ArrayList<ItemStack>(),0,null);
            }
        }
        throw new IllegalStateException("Death event constructor missing");
    }

    private Map<String,Object> state(Player player) {
        int stones=0; for(ItemStack item:player.getInventory().getContents()) if(item!=null&&item.getType()==Material.COBBLESTONE) stones+=item.getAmount();
        GodAbility ability=core.abilities().session(player)==null?null:core.abilities().session(player).ability();
        int displays=0;for(Entity entity:player.getWorld().getEntities()) if(entity.getType().name().endsWith("DISPLAY")) displays++;
        return map("artPackLoaded",core.effectArtPack().ready(player),"ability",current,"stones",stones,"health",player.getHealth(),"dead",player.isDead(),"displays",displays,
            "normalCooldown",ability==null?0:ability.cooldownRemainingMillis(1),"advancedCooldown",ability==null?0:ability.cooldownRemainingMillis(2),
            "x",player.getLocation().getX(),"y",player.getLocation().getY(),"z",player.getLocation().getZ(),"target",target==null?null:target.getName(),
            "targetHealth",target==null?null:target.getHealth(),"targetY",target==null?null:target.getLocation().getY());
    }

    private static Map<String,Object> map(Object... pairs) {
        Map<String,Object> result=new LinkedHashMap<String,Object>();
        for(int i=0;i<pairs.length;i+=2) result.put(String.valueOf(pairs[i]),pairs[i+1]);return result;
    }
    private static String json(Object value) {
        if(value==null) return "null";
        if(value instanceof Number || value instanceof Boolean) return value.toString();
        if(value instanceof Map) { List<String> out=new ArrayList<String>();for(Object entry:((Map<?,?>)value).entrySet()) { Map.Entry<?,?> e=(Map.Entry<?,?>)entry;out.add(json(e.getKey())+":"+json(e.getValue())); }return "{"+String.join(",",out)+"}"; }
        if(value instanceof Iterable) {List<String> out=new ArrayList<String>();for(Object v:(Iterable<?>)value)out.add(json(v));return "["+String.join(",",out)+"]";}
        String s=String.valueOf(value);StringBuilder out=new StringBuilder("\"");
        for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='"'||c=='\\')out.append('\\').append(c);else if(c<32)out.append(String.format("\\u%04x",(int)c));else out.append(c);}return out.append('"').toString();
    }

    @Override public void onDisable() { clear(); }
}
