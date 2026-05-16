package kr.newgodwar.ability.builtin;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityFactory;
import kr.newgodwar.ability.api.AbilityKillContext;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.GodAbility;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.util.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class TheomachyAbilities {

    private TheomachyAbilities() {
    }

    public static Collection<AbilityDefinition> definitions() {
        List<AbilityDefinition> definitions = new ArrayList<AbilityDefinition>();
        add(definitions, "zeus", "제우스", "번개를 내리고 번개/폭발 피해를 무시합니다.");
        add(definitions, "hades", "하데스", "주변 생물을 나락으로 떨어뜨리고 사망 시 확률로 아이템을 보존합니다.");
        add(definitions, "demeter", "데메테르", "빵을 만들고 허기와 재생에 강합니다.");
        add(definitions, "athena", "아테나", "책과 인챈트 테이블을 만들고 사망자를 통해 경험을 얻습니다.");
        add(definitions, "apollon", "아폴론", "태양을 띄우고 주변 적을 불태웁니다.");
        add(definitions, "artemis", "아르테미스", "화살과 활을 만들며 화살로 즉사 확률을 가집니다.");
        add(definitions, "ares", "아레스", "공격 피해가 증가하고 일정 확률로 공격을 회피합니다.");
        add(definitions, "hephaestus", "헤파이토스", "용암을 만들고 화염 피해를 무시합니다.");
        add(definitions, "asclepius", "아스클리피어스", "자신 또는 주변 아군을 완전히 회복합니다.");
        add(definitions, "hermes", "헤르메스", "빠른 이동과 짧은 비행을 사용합니다.");
        add(definitions, "dionysus", "디오니소스", "피격 시 확률로 공격자를 취하게 합니다.");
        add(definitions, "aprodite", "아프로디테", "주변 플레이어를 자신의 위치로 끌어옵니다.");
        add(definitions, "eris", "에리스", "피격 시 확률로 공격자를 밀쳐냅니다.");
        add(definitions, "morpious", "모르피우스", "지정한 적을 수면 상태로 만듭니다.");
        add(definitions, "aeolus", "아이올로스", "아군에게 바람의 축복을 주거나 적을 밀쳐냅니다.");
        add(definitions, "akasha", "아카샤", "아군에게 향락을, 적에게 고통을 부여합니다.");
        add(definitions, "horeundal", "호른달", "현재 위치를 기억하고 잠시 후 되돌아옵니다.");
        add(definitions, "jujak", "주작", "화염 피해를 무시하고 짧게 비행합니다.");
        add(definitions, "frost", "잭프로스트", "얼음을 만들고 지정한 적을 얼음 안에 가둡니다.");
        add(definitions, "nasdaq", "나스닥", "철괴나 다이아몬드를 확률적으로 복사합니다.");

        add(definitions, "archer", "아처", "화살/활을 만들고 활 피해가 증가합니다.");
        add(definitions, "miner", "광부", "코블스톤 채굴 보너스와 곡괭이 고정 피해를 가집니다.");
        add(definitions, "stance", "스탠스", "공격 넉백과 피해 증폭을 무시합니다.");
        add(definitions, "teleporter", "텔레포터", "바라보는 곳으로 이동하거나 아군과 위치를 바꿉니다.");
        add(definitions, "bomber", "봄버", "보이지 않는 폭탄을 설치하고 원격 폭발시킵니다.");
        add(definitions, "creeper", "크리퍼", "자폭하고 번개를 맞으면 폭발력이 달라집니다.");
        add(definitions, "wizard", "마법사", "주변 플레이어를 날리거나 신의 심판을 내립니다.");
        add(definitions, "assasin", "암살자", "더블 점프와 기습 이동을 사용합니다.");
        add(definitions, "reflection", "반사", "피격 시 확률로 받은 피해를 반사합니다.");
        add(definitions, "blinder", "블라인더", "주변 적이나 공격자에게 실명을 겁니다.");
        add(definitions, "invincibility", "무적", "잠시 무적이 되거나 재생 효과를 얻습니다.");
        add(definitions, "clocking", "클로킹", "투명화 후 공격 시 확률로 즉사시킵니다.");
        add(definitions, "blacksmith", "대장장이", "코블스톤을 철로, 철을 다이아몬드로 바꿉니다.");
        add(definitions, "priest", "사제", "자신 또는 팀원에게 무작위 축복을 부여합니다.");
        add(definitions, "witch", "마녀", "주변 적과 공격자에게 저주를 겁니다.");
        add(definitions, "sniper", "저격수", "스나이핑 모드에서 매우 빠른 화살을 발사합니다.");
        add(definitions, "voodoo", "부두술사", "팻말로 대상을 연결해 원격 피해를 줍니다.");
        add(definitions, "anorexia", "거식증", "허기가 절반으로 유지됩니다.");
        add(definitions, "bulter", "집사", "폭발을 안정시켜 막습니다.");
        add(definitions, "midoriya", "미도리야", "원 포 올을 준비한 뒤 맨손 공격으로 큰 피해를 줍니다.");
        add(definitions, "goldspoon", "금수저", "리스폰할 때 레깅스를 받습니다.");
        add(definitions, "queenbee", "여왕벌", "독침과 페로몬 끌어오기를 사용합니다.");
        add(definitions, "snow", "사이코스노우", "눈덩이 피해와 성장하는 공격 지수를 가집니다.");
        add(definitions, "tajja", "타짜", "검을 숨겨 맨손 공격에 검 피해를 싣습니다.");
        add(definitions, "girl", "안락소녀", "주변 적을 끌어와 굶주리게 합니다.");
        add(definitions, "megumin", "메구밍", "게임 중 한 번 폭렬 마법을 사용합니다.");
        add(definitions, "pokego", "포켓몬고", "많이 걸으면 다른 능력으로 바뀝니다.");
        add(definitions, "darkness", "다크니스", "받는 피해를 줄이고 자신 공격은 피해가 없습니다.");
        add(definitions, "gasolin", "가솔린기관", "화염 피해를 받으면 빨라집니다.");
        add(definitions, "zet", "제트기관", "화염 피해를 받으면 높은 속도로 가속합니다.");
        add(definitions, "hermione", "헤르미온느", "채팅 주문으로 다양한 마법을 사용합니다.");
        add(definitions, "harry", "해리포터", "헤르미온느보다 숙련된 채팅 주문 마법을 사용합니다.");
        add(definitions, "gardener", "정원사", "나무를 캐면 꽃과 코블스톤을 얻습니다.");
        add(definitions, "acidarcher", "독화살아처", "활 피해 대신 독을 부여합니다.");
        add(definitions, "galbi", "명륜진사", "익힌 돼지고기를 만들고 허기와 재생에 강합니다.");
        add(definitions, "naro", "나로호", "낙하 피해를 무시하고 크게 도약합니다.");
        add(definitions, "scrooge", "스크루지", "팀원의 능력 코블스톤 비용을 절반으로 낮춥니다.");
        add(definitions, "fisher", "노인과바다", "낚시로 잡동사니와 광물을 얻습니다.");
        add(definitions, "examinee", "수험생", "수학 문제를 맞히면 무작위 능력으로 바뀝니다.");
        return definitions;
    }

    private static void add(List<AbilityDefinition> definitions, final String id, String name, String description) {
        definitions.add(new AbilityDefinition(id, name, description, "Theomachy", true, new AbilityFactory() {
            @Override
            public GodAbility create() {
                return new Scripted(id);
            }
        }));
    }

    private static final class Scripted implements GodAbility {
        private static final Material COBBLESTONE = Material.COBBLESTONE;
        private static final Material STAFF = Material.BLAZE_ROD;
        private static final Random RANDOM = new Random();
        private static final Set<String> SCROOGE_TEAMS = new HashSet<String>();

        private final String id;
        private final Map<Integer, Long> cooldowns = new LinkedHashMap<Integer, Long>();
        private String targetName;
        private Location bombLocation;
        private boolean ready;
        private boolean oneTimeUsed;
        private boolean plasma;
        private boolean invisible;
        private boolean invincible;
        private int snowAttack;
        private int tajjaDamage;
        private int tajjaUses;
        private int enchantTables = 2;
        private ItemStack[] savedInventory;
        private ItemStack[] savedArmor;
        private int steps;
        private String pendingQuestion;
        private int pendingAnswer = -1;

        private Scripted(String id) {
            this.id = id;
        }

        @Override
        public void onAssign(AbilityPlayerContext context) {
            Player player = context.player();
            if (id.equals("hermes")) {
                effect(player, PotionEffectType.SPEED, 24 * 60 * 60, 0);
            } else if (id.equals("demeter") || id.equals("galbi")) {
                effect(player, PotionEffectType.REGENERATION, 24 * 60 * 60, 0);
                player.setFoodLevel(20);
            } else if (id.equals("fisher")) {
                give(player, Material.FISHING_ROD, 1);
            } else if (id.equals("gardener")) {
                give(player, Material.SAPLING, 5);
                player.getInventory().addItem(new ItemStack(351, 1, (short) 10));
            } else if (id.equals("sniper")) {
                give(player, Material.BOW, 1);
                give(player, Material.ARROW, 10);
            } else if (id.equals("snow")) {
                give(player, Material.SNOW_BALL, 1);
            } else if (id.equals("anorexia")) {
                player.setFoodLevel(10);
            } else if (id.equals("scrooge")) {
                GodTeam team = context.plugin().game().teamOf(player);
                if (team != null) {
                    SCROOGE_TEAMS.add(team.id());
                }
            } else if (id.equals("harry") || id.equals("hermione")) {
                giveSpellBook(player, id.equals("harry"));
            }
        }

        @Override
        public void onRemove(AbilityPlayerContext context) {
            Player player = context.player();
            if (id.equals("hermes")) {
                player.removePotionEffect(PotionEffectType.SPEED);
            } else if (id.equals("demeter") || id.equals("galbi")) {
                player.removePotionEffect(PotionEffectType.REGENERATION);
            } else if (id.equals("clocking")) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }

        @Override
        public void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {
            Player player = context.player();
            Action action = event.getAction();
            boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
            boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

            if (id.equals("nasdaq") && left && (holding(player, Material.IRON_INGOT) || holding(player, Material.DIAMOND))) {
                nasdaq(player);
                return;
            }
            if (id.equals("sniper") && left && holding(player, Material.BOW)) {
                sniperReady(player);
                return;
            }
            if (!holding(player, STAFF)) {
                return;
            }

            if (left) {
                leftSkill(context, player, event);
            } else if (right) {
                rightSkill(context, player, event);
            }
        }

        private void leftSkill(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
            if (id.equals("zeus") && use(context, player, 1, COBBLESTONE, 15, 90)) {
                player.getWorld().strikeLightning(targetBlock(player, 50).getLocation());
            } else if (id.equals("hades") && use(context, player, 1, COBBLESTONE, 20, 100)) {
                abyss(player, 2, true);
            } else if (id.equals("demeter") && use(context, player, 0, COBBLESTONE, 10, 15)) {
                give(player, Material.BREAD, 10);
            } else if (id.equals("athena") && use(context, player, 1, COBBLESTONE, 5, 10)) {
                give(player, Material.BOOK, 3);
            } else if (id.equals("apollon") && use(context, player, 1, COBBLESTONE, 1, 30)) {
                player.getWorld().setTime(6000);
                Bukkit.broadcastMessage(ChatColor.YELLOW + "태양의 신이 해를 띄웠습니다.");
            } else if ((id.equals("artemis") || id.equals("archer") || id.equals("acidarcher")) && use(context, player, 1, COBBLESTONE, id.equals("artemis") ? 7 : 5, 20)) {
                give(player, Material.ARROW, 1);
            } else if (id.equals("hephaestus")) {
                lava(player, context);
            } else if (id.equals("asclepius") && use(context, player, 1, COBBLESTONE, 10, 60)) {
                heal(player);
            } else if ((id.equals("hermes") || id.equals("jujak")) && use(context, player, 0, COBBLESTONE, id.equals("hermes") ? 10 : 15, id.equals("hermes") ? 60 : 80)) {
                fly(context, player, id.equals("hermes") ? 7 : 12);
            } else if (id.equals("aprodite") && use(context, player, 0, COBBLESTONE, 20, 100)) {
                pullAll(player, 20);
            } else if (id.equals("morpious") && use(context, player, 0, COBBLESTONE, 20, 100)) {
                sleepTarget(context, player);
            } else if ((id.equals("aeolus") || id.equals("akasha")) && use(context, player, 1, COBBLESTONE, 10, 60)) {
                teamBuff(context, player);
            } else if (id.equals("horeundal") && use(context, player, 0, COBBLESTONE, 15, 80)) {
                recall(context, player);
            } else if (id.equals("frost") && use(context, player, 1, COBBLESTONE, 10, 8)) {
                iceSphere(context, targetBlock(player, 15).getLocation(), 3, 5);
            } else if (id.equals("teleporter") && use(context, player, 1, COBBLESTONE, 15, 25)) {
                teleportToTargetBlock(player);
            } else if (id.equals("bomber")) {
                bombLocation = targetBlock(player, 5).getLocation().add(0, 1, 0);
                player.sendMessage("해당 블럭에 폭탄이 설치되었습니다.");
            } else if (id.equals("creeper") && use(context, player, 0, COBBLESTONE, 20, 60)) {
                player.getWorld().createExplosion(player.getLocation(), plasma ? 6.0F : 3.0F);
                player.setHealth(0.0D);
            } else if (id.equals("wizard") && use(context, player, 1, COBBLESTONE, 5, 180)) {
                push(player, nearbyPlayers(context, player, 10, false), 2.4D);
            } else if (id.equals("assasin")) {
                dash(player);
            } else if (id.equals("blinder") && use(context, player, 0, COBBLESTONE, 10, 30)) {
                for (Player target : nearbyPlayers(context, player, 5, false)) {
                    effect(target, PotionEffectType.BLINDNESS, 8, 0);
                }
            } else if (id.equals("invincibility") && use(context, player, 1, COBBLESTONE, 30, 50)) {
                invincible = true;
                later(context, 7, new Runnable() {
                    @Override
                    public void run() {
                        invincible = false;
                    }
                });
            } else if (id.equals("clocking") && use(context, player, 0, COBBLESTONE, 25, 60)) {
                invisible = true;
                effect(player, PotionEffectType.INVISIBILITY, 7, 0);
                later(context, 7, new Runnable() {
                    @Override
                    public void run() {
                        invisible = false;
                    }
                });
            } else if (id.equals("blacksmith") && use(context, player, 1, COBBLESTONE, 70, 300)) {
                give(player, Material.IRON_INGOT, 10);
            } else if (id.equals("priest") && use(context, player, 1, COBBLESTONE, 30, 35)) {
                bless(player);
            } else if (id.equals("witch") && use(context, player, 0, COBBLESTONE, 15, 60)) {
                curse(nearbyPlayers(context, player, 10, false));
            } else if (id.equals("midoriya") && has(player, COBBLESTONE, cost(context, 50)) && readyCooldown(player, 0, 150)) {
                ready = true;
                player.sendMessage(ChatColor.YELLOW + "원" + ChatColor.GREEN + " 포 " + ChatColor.AQUA + "올" + ChatColor.WHITE + "이 준비되었습니다!");
            } else if (id.equals("queenbee") && use(context, player, 0, COBBLESTONE, 30, 150)) {
                pullTarget(context, player, 10);
            } else if (id.equals("snow")) {
                giveSnow(player, context);
            } else if (id.equals("tajja") && use(context, player, 0, COBBLESTONE, 10, 60)) {
                stealSword(player);
            } else if (id.equals("girl") && use(context, player, 0, COBBLESTONE, 15, 60)) {
                for (Player target : nearbyPlayers(context, player, 5, false)) {
                    target.teleport(player);
                    target.setFoodLevel(0);
                }
            } else if (id.equals("megumin")) {
                explosionMagic(context, player);
            } else if (id.equals("naro") && use(context, player, 0, COBBLESTONE, 2, 10)) {
                Vector vector = player.getEyeLocation().getDirection();
                vector.setY(3.0D);
                player.setVelocity(vector);
            } else if (id.equals("examinee") && use(context, player, 0, COBBLESTONE, 5, 60)) {
                askQuestion(player);
            } else if (id.equals("galbi") && use(context, player, 0, COBBLESTONE, 10, 20)) {
                give(player, Material.GRILLED_PORK, 3);
            }
        }

        private void rightSkill(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
            if (id.equals("zeus") && use(context, player, 2, COBBLESTONE, 25, 150)) {
                Location center = targetBlock(player, 30).getLocation();
                for (int i = 0; i < 5; i++) {
                    player.getWorld().strikeLightning(center.clone().add(RANDOM.nextInt(11) - 5, 0, RANDOM.nextInt(11) - 5));
                }
            } else if (id.equals("hades") && use(context, player, 2, COBBLESTONE, 35, 150)) {
                abyss(player, 4, false);
            } else if (id.equals("athena")) {
                enchantTable(context, player);
            } else if (id.equals("apollon") && use(context, player, 2, COBBLESTONE, 15, 90)) {
                player.getWorld().setTime(6000);
                for (Player target : nearbyPlayers(context, player, 15, false)) {
                    if (target.getLocation().getBlock().getLightFromSky() > 10) {
                        target.setFireTicks(80);
                        target.damage(2.0D, player);
                    }
                }
            } else if ((id.equals("artemis") || id.equals("archer") || id.equals("acidarcher")) && use(context, player, 2, COBBLESTONE, id.equals("artemis") ? 15 : 15, id.equals("artemis") ? 180 : 60)) {
                give(player, Material.BOW, 1);
            } else if (id.equals("asclepius") && use(context, player, 2, COBBLESTONE, 15, 120)) {
                for (Player target : nearbyPlayers(context, player, 5, true)) {
                    heal(target);
                }
            } else if (id.equals("frost") && use(context, player, 2, COBBLESTONE, 20, 140)) {
                Player target = targetPlayer();
                if (target != null && !sameTeam(context, player, target)) {
                    iceSphere(context, target.getLocation(), 5, 8);
                }
            } else if (id.equals("teleporter") && use(context, player, 2, COBBLESTONE, 25, 30)) {
                swapTarget(context, player);
            } else if (id.equals("bomber") && use(context, player, 0, COBBLESTONE, 25, 30)) {
                if (bombLocation != null) {
                    player.getWorld().createExplosion(bombLocation, 2.0F, true);
                    bombLocation = null;
                }
            } else if (id.equals("wizard") && use(context, player, 2, COBBLESTONE, 10, 300)) {
                judgment(context, player);
            } else if (id.equals("assasin") && use(context, player, 2, COBBLESTONE, 15, 15)) {
                backstab(context, player);
            } else if (id.equals("invincibility") && use(context, player, 2, COBBLESTONE, 50, 90)) {
                effect(player, PotionEffectType.REGENERATION, 25, 0);
            } else if (id.equals("blacksmith") && use(context, player, 2, Material.IRON_INGOT, 20, 600)) {
                give(player, Material.DIAMOND, 5);
            } else if (id.equals("priest") && use(context, player, 2, COBBLESTONE, 45, 90)) {
                for (Player target : nearbyPlayers(context, player, 30, true)) {
                    bless(target);
                }
            } else if ((id.equals("akasha") || id.equals("aeolus")) && use(context, player, 2, COBBLESTONE, 20, id.equals("akasha") ? 80 : 150)) {
                if (id.equals("akasha")) {
                    for (Player target : nearbyPlayers(context, player, 10, false)) {
                        effect(target, PotionEffectType.CONFUSION, 6, 0);
                        damage(target, 4.0D, player);
                    }
                } else {
                    List<Player> targets = nearbyPlayers(context, player, 10, false);
                    push(player, targets, 2.4D);
                    for (Player target : targets) {
                        effect(target, PotionEffectType.WEAKNESS, 5, 0);
                        effect(target, PotionEffectType.SLOW, 5, 0);
                    }
                }
            } else if (id.equals("snow")) {
                player.sendMessage("공격 지수 : " + snowAttack);
            }
        }

        @Override
        public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
            Player owner = context.player();
            if (id.equals("ares")) {
                if (attacker) {
                    event.setDamage(event.getDamage() * 1.5D);
                } else if (RANDOM.nextInt(10) == 0) {
                    event.setCancelled(true);
                    owner.sendMessage("회피했습니다!");
                }
            } else if (id.equals("dionysus") && !attacker && RANDOM.nextInt(20) <= 2) {
                effect(opponent, PotionEffectType.SLOW, 10, 0);
                effect(opponent, PotionEffectType.WEAKNESS, 10, 0);
                effect(opponent, PotionEffectType.CONFUSION, 12, 0);
            } else if (id.equals("eris") && !attacker && RANDOM.nextInt(5) == 0) {
                opponent.teleport(owner.getLocation().add(5, 0, 5));
            } else if (id.equals("blinder") && !attacker && RANDOM.nextInt(10) == 0) {
                effect(opponent, PotionEffectType.BLINDNESS, 4, 0);
            } else if (id.equals("witch") && !attacker && RANDOM.nextInt(14) == 0) {
                curse(opponent);
            } else if (id.equals("queenbee") && !attacker && RANDOM.nextBoolean()) {
                effect(opponent, PotionEffectType.POISON, 5, 0);
            } else if (id.equals("reflection") && !attacker && RANDOM.nextBoolean()) {
                opponent.damage(event.getDamage(), owner);
            } else if (id.equals("darkness")) {
                event.setDamage(attacker ? 0.0D : event.getDamage() / 10.0D);
            } else if (id.equals("boxer") && attacker && owner.getItemInHand().getType() == Material.AIR) {
                opponent.setNoDamageTicks(0);
            } else if (id.equals("miner") && attacker && isPickaxe(owner.getItemInHand().getType())) {
                event.setDamage(4.0D);
            } else if (id.equals("midoriya") && attacker && ready && owner.getItemInHand().getType() == Material.AIR) {
                if (use(context, owner, 0, COBBLESTONE, 50, 150)) {
                    ready = false;
                    event.setDamage(200.0D);
                    effect(owner, PotionEffectType.CONFUSION, 10, 0);
                    effect(owner, PotionEffectType.HUNGER, 10, 0);
                    effect(owner, PotionEffectType.WEAKNESS, 10, 0);
                    effect(owner, PotionEffectType.SLOW, 10, 0);
                }
            } else if (id.equals("clocking") && attacker && invisible) {
                owner.removePotionEffect(PotionEffectType.INVISIBILITY);
                invisible = false;
                if (RANDOM.nextInt(5) == 0) {
                    event.setDamage(100.0D);
                }
            } else if (id.equals("tajja") && attacker && owner.getItemInHand().getType() == Material.AIR && tajjaDamage > 0) {
                event.setDamage(tajjaDamage);
                tajjaUses--;
                if (tajjaUses <= 0) {
                    tajjaDamage = 0;
                }
            }
        }

        @Override
        public void onProjectileHit(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player victim) {
            if (id.equals("artemis") && RANDOM.nextInt(20) <= 2) {
                event.setDamage(100.0D);
            } else if (id.equals("archer")) {
                event.setDamage(event.getDamage() * 1.3D);
            } else if (id.equals("acidarcher")) {
                event.setDamage(0.0D);
                effect(victim, PotionEffectType.POISON, 10, 0);
            } else if (id.equals("snow")) {
                Projectile projectile = (Projectile) event.getDamager();
                if (projectile.getType().name().contains("SNOW")) {
                    event.setCancelled(true);
                    damage(victim, Math.max(1, snowAttack), context.player());
                }
            }
        }

        @Override
        public void onProjectileLaunch(AbilityPlayerContext context, ProjectileLaunchEvent event) {
            if (id.equals("sniper") && ready && event.getEntity() instanceof Arrow && use(context, context.player(), 0, COBBLESTONE, 5, 50)) {
                ready = false;
                event.getEntity().setVelocity(context.player().getEyeLocation().getDirection().multiply(20));
            }
        }

        @Override
        public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
            EntityDamageEvent.DamageCause cause = event.getCause();
            if ((id.equals("zeus") && (cause == EntityDamageEvent.DamageCause.LIGHTNING || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION))
                || ((id.equals("hephaestus") || id.equals("jujak")) && fire(cause))) {
                event.setCancelled(true);
                context.player().setFireTicks(0);
            } else if ((id.equals("hephaestus") || id.equals("jujak")) && cause == EntityDamageEvent.DamageCause.DROWNING) {
                event.setDamage(event.getDamage() * 2.0D);
            } else if (id.equals("snow") && fire(cause)) {
                event.setDamage(event.getDamage() * 2.0D);
            } else if (id.equals("gasolin") && fire(cause) && !context.player().hasPotionEffect(PotionEffectType.SPEED)) {
                effect(context.player(), PotionEffectType.SPEED, 10, 0);
            } else if (id.equals("zet") && fire(cause) && !context.player().hasPotionEffect(PotionEffectType.SPEED) && RANDOM.nextBoolean()) {
                effect(context.player(), PotionEffectType.SPEED, 5, 1);
            } else if (id.equals("naro") && cause == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
            } else if (id.equals("invincibility") && invincible) {
                event.setCancelled(true);
                context.player().setFireTicks(0);
            } else if ((id.equals("harry") || id.equals("hermione")) && invincible) {
                event.setCancelled(true);
            } else if (id.equals("stance") && (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.PROJECTILE)) {
                double damage = event.getDamage();
                event.setCancelled(true);
                context.player().damage(damage);
            } else if (id.equals("creeper") && cause == EntityDamageEvent.DamageCause.LIGHTNING) {
                plasma = true;
            }
        }

        @Override
        public void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
            if (id.equals("athena") && !event.getEntity().equals(context.player())) {
                context.player().setLevel(context.player().getLevel() + 1);
            } else if (id.equals("hades") && event.getEntity().equals(context.player()) && RANDOM.nextInt(10) <= 6) {
                savedInventory = context.player().getInventory().getContents();
                savedArmor = context.player().getInventory().getArmorContents();
                event.getDrops().clear();
            } else if (id.equals("snow") && event.getEntity().equals(context.player()) && snowAttack < 8) {
                snowAttack++;
            } else if (id.equals("creeper") && event.getEntity().equals(context.player())) {
                plasma = false;
            }
        }

        @Override
        public void onKill(AbilityKillContext context) {
            if (id.equals("poseidon")) {
                return;
            }
        }

        @Override
        public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
            Player player = context.player();
            if (id.equals("hades")) {
                if (savedInventory != null) {
                    player.getInventory().setContents(savedInventory);
                    savedInventory = null;
                }
                if (savedArmor != null) {
                    player.getInventory().setArmorContents(savedArmor);
                    savedArmor = null;
                }
            } else if (id.equals("goldspoon")) {
                give(player, RANDOM.nextInt(10) < 9 ? Material.GOLD_LEGGINGS : Material.DIAMOND_LEGGINGS, 1);
            } else if (id.equals("hermes")) {
                effect(player, PotionEffectType.SPEED, 24 * 60 * 60, 0);
            } else if (id.equals("demeter") || id.equals("galbi")) {
                effect(player, PotionEffectType.REGENERATION, 24 * 60 * 60, 0);
            }
        }

        @Override
        public void onFoodLevelChange(AbilityPlayerContext context, FoodLevelChangeEvent event) {
            if (id.equals("demeter") || id.equals("galbi")) {
                event.setCancelled(true);
                context.player().setFoodLevel(20);
            } else if (id.equals("anorexia")) {
                event.setFoodLevel(10);
            }
        }

        @Override
        public void onBlockBreak(AbilityPlayerContext context, BlockBreakEvent event) {
            if (id.equals("miner") && event.getBlock().getType() == Material.COBBLESTONE && RANDOM.nextInt(33) == 0) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.COBBLESTONE, 9));
            } else if (id.equals("gardener") && event.getBlock().getType() == Material.LOG) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.RED_ROSE, 1));
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.COBBLESTONE, 1));
            }
        }

        @Override
        public void onBlockPlace(AbilityPlayerContext context, BlockPlaceEvent event) {
            if (id.equals("voodoo") && event.getBlock().getType() == Material.SIGN && (!readyCooldown(context.player(), 0, 180) || !has(context.player(), COBBLESTONE, cost(context, 5)))) {
                event.setCancelled(true);
            }
        }

        @Override
        public void onSignChange(AbilityPlayerContext context, SignChangeEvent event) {
            if (id.equals("voodoo")) {
                Player target = Bukkit.getPlayer(event.getLine(0));
                if (target != null && use(context, context.player(), 0, COBBLESTONE, 5, 180)) {
                    targetName = target.getName();
                    final Block sign = event.getBlock();
                    later(context, 7, new Runnable() {
                        @Override
                        public void run() {
                            targetName = null;
                            sign.breakNaturally();
                        }
                    });
                }
            }
        }

        @Override
        public void onBlockExplode(BlockExplodeEvent event) {
            if (id.equals("bulter")) {
                event.setCancelled(true);
                Bukkit.broadcastMessage(ChatColor.GREEN + "집사에 의해 폭발이 진정되었습니다.");
            }
        }

        @Override
        public void onMove(AbilityPlayerContext context, PlayerMoveEvent event) {
            if (id.equals("pokego") && event.getFrom().distanceSquared(event.getTo()) > 0.01D) {
                steps++;
                if (steps >= 1000) {
                    steps = 0;
                    context.plugin().abilities().assignRandom(context.player());
                    context.player().sendMessage(ChatColor.AQUA + "새 능력을 잡았습니다!");
                }
            }
        }

        @Override
        public void onChat(AbilityPlayerContext context, AsyncPlayerChatEvent event) {
            if (id.equals("examinee")) {
                answerQuestion(context, event);
            } else if (id.equals("harry") || id.equals("hermione")) {
                castSpell(context, event.getMessage(), id.equals("harry"));
            }
        }

        @Override
        public void onFish(AbilityPlayerContext context, PlayerFishEvent event) {
            if (id.equals("fisher") && event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                if (event.getCaught() != null) {
                    event.getCaught().remove();
                }
                int roll = RANDOM.nextInt(100);
                if (roll <= 4) {
                    give(context.player(), Material.DIAMOND, 1);
                } else if (roll <= 19) {
                    give(context.player(), Material.LOG, 3);
                } else if (roll <= 34) {
                    give(context.player(), STAFF, 1);
                } else if (roll <= 98) {
                    give(context.player(), Material.IRON_INGOT, roll <= 79 ? 1 : 2);
                } else {
                    give(context.player(), Material.DIAMOND, 2);
                }
            }
        }

        @Override
        public void setTarget(AbilityPlayerContext context, CommandSender sender, String targetName) {
            if (context.player().getName().equalsIgnoreCase(targetName)) {
                sender.sendMessage("자기 자신을 타깃으로 등록 할 수 없습니다.");
                return;
            }
            this.targetName = targetName;
            sender.sendMessage("타깃을 등록했습니다.   " + ChatColor.GREEN + targetName);
        }

        private void castSpell(AbilityPlayerContext context, String spell, boolean harry) {
            Player player = context.player();
            if (spell.equals("루모스") || spell.equalsIgnoreCase("Lumos")) {
                if (use(context, player, 1, COBBLESTONE, 5, 5)) {
                    player.getWorld().setTime(1000);
                }
            } else if (spell.equals("녹스") || spell.equalsIgnoreCase("Nox")) {
                if (use(context, player, 1, COBBLESTONE, 5, 5)) {
                    player.getWorld().setTime(18000);
                }
            } else if (spell.equals("봄바르다") || spell.equalsIgnoreCase("Bombarda")) {
                if (use(context, player, 1, COBBLESTONE, 5, 5)) {
                    player.getWorld().createExplosion(targetBlock(player, 5).getLocation(), 1.0F);
                }
            } else if (spell.equals("스투페파이") || spell.equalsIgnoreCase("Stupefy")) {
                if (use(context, player, 2, COBBLESTONE, 10, 20)) {
                    for (Player target : nearbyPlayers(context, player, 10, false)) {
                        if (RANDOM.nextBoolean()) {
                            effect(target, PotionEffectType.SLOW, 8, harry ? 1 : 2);
                        }
                    }
                }
            } else if (spell.equals("익스펙토 패트로눔") || spell.equalsIgnoreCase("Expecto Patronum")) {
                if (use(context, player, 2, COBBLESTONE, 10, 20) && RANDOM.nextInt(4) < (harry ? 3 : 2)) {
                    invincible = true;
                    later(context, 5, new Runnable() {
                        @Override
                        public void run() {
                            invincible = false;
                        }
                    });
                }
            } else if (spell.equals("엑스펠리아무스") || spell.equalsIgnoreCase("Expelliarmus")) {
                Player target = targetPlayer();
                if (target != null && use(context, player, 2, COBBLESTONE, 10, 20) && RANDOM.nextInt(100) < (harry ? 25 : 20)) {
                    target.getInventory().setArmorContents(new ItemStack[] {null, null, null, null});
                    target.setItemInHand(new ItemStack(Material.AIR));
                }
            } else if (spell.equals("아바다 케다브라") || spell.equalsIgnoreCase("Avada Kedavra")) {
                Player target = targetPlayer();
                if (target != null && use(context, player, 2, COBBLESTONE, 10, 20) && RANDOM.nextInt(100) < (harry ? 20 : 15)) {
                    target.setHealth(0.0D);
                }
            }
        }

        private boolean use(AbilityPlayerContext context, Player player, int slot, Material material, int amount, int cooldownSeconds) {
            int realCost = material == COBBLESTONE ? cost(context, amount) : amount;
            if (!readyCooldown(player, slot, cooldownSeconds) || !has(player, material, realCost)) {
                return false;
            }
            if (realCost > 0) {
                player.getInventory().removeItem(new ItemStack(material, realCost));
            }
            cooldowns.put(slot, System.currentTimeMillis() + context.plugin().abilities().scaleCooldownMillis(cooldownSeconds * 1000L));
            player.sendMessage(ChatColor.GREEN + "능력을 사용했습니다.");
            return true;
        }

        private boolean readyCooldown(Player player, int slot, int cooldownSeconds) {
            Long until = cooldowns.get(slot);
            long now = System.currentTimeMillis();
            if (until != null && until > now) {
                player.sendMessage(ChatColor.YELLOW + "쿨타임: " + ((until - now + 999L) / 1000L) + "초");
                return false;
            }
            return true;
        }

        private int cost(AbilityPlayerContext context, int amount) {
            GodTeam team = context.plugin().game().teamOf(context.player());
            if (team != null && SCROOGE_TEAMS.contains(team.id())) {
                return Math.max(0, amount / 2);
            }
            return amount;
        }

        private boolean has(Player player, Material material, int amount) {
            if (amount <= 0 || player.getInventory().contains(material, amount)) {
                return true;
            }
            player.sendMessage(ChatColor.RED + material.name() + " " + amount + "개가 필요합니다.");
            return false;
        }

        private boolean holding(Player player, Material material) {
            return player.getItemInHand() != null && player.getItemInHand().getType() == material;
        }

        private void give(Player player, Material material, int amount) {
            player.getInventory().addItem(new ItemStack(material, amount));
        }

        private void effect(Player player, PotionEffectType type, int seconds, int amplifier) {
            BukkitCompat.addPotionEffect(player, type, seconds * 20, amplifier, true, false);
        }

        private void heal(Player player) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getMaxHealth()));
        }

        private void damage(Player target, double amount, Player source) {
            target.damage(Math.min(target.getHealth(), amount), source);
        }

        private boolean fire(EntityDamageEvent.DamageCause cause) {
            return cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.LAVA;
        }

        private Block targetBlock(Player player, int range) {
            return BukkitCompat.getTargetBlock(player, range);
        }

        private List<Player> nearbyPlayers(AbilityPlayerContext context, Player player, int range, boolean sameTeam) {
            List<Player> players = new ArrayList<Player>();
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (entity instanceof Player) {
                    Player target = (Player) entity;
                    if (sameTeam == sameTeam(context, player, target)) {
                        players.add(target);
                    }
                }
            }
            return players;
        }

        private boolean sameTeam(AbilityPlayerContext context, Player a, Player b) {
            GodTeam first = context.plugin().game().teamOf(a);
            GodTeam second = context.plugin().game().teamOf(b);
            return first != null && first == second;
        }

        private Player targetPlayer() {
            return targetName == null ? null : Bukkit.getPlayer(targetName);
        }

        private void later(AbilityPlayerContext context, int seconds, Runnable runnable) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(context.plugin(), runnable, seconds * 20L);
        }

        private void abyss(Player player, int radius, boolean includeSelf) {
            Location destination = player.getLocation().clone();
            destination.setY(-2.0D);
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity) {
                    entity.teleport(destination);
                }
            }
            if (includeSelf) {
                player.teleport(destination);
            }
        }

        private void lava(Player player, AbilityPlayerContext context) {
            Block base = targetBlock(player, 5);
            final Block block = base.getLocation().add(0, 1, 0).getBlock();
            if (block.getType() == Material.AIR && use(context, player, 0, COBBLESTONE, 1, 10)) {
                block.setType(Material.LAVA);
                later(context, 2, new Runnable() {
                    @Override
                    public void run() {
                        if (block.getType() == Material.LAVA) {
                            block.setType(Material.AIR);
                        }
                    }
                });
            }
        }

        private void fly(final AbilityPlayerContext context, final Player player, int seconds) {
            player.setAllowFlight(true);
            player.setFlying(true);
            later(context, seconds, new Runnable() {
                @Override
                public void run() {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
            });
        }

        private void pullAll(Player player, int range) {
            if (player.isSneaking() || player.getLocation().clone().add(0, -1, 0).getBlock().getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "웅크리고 있거나 발 밑의 블록이 없어 능력이 발동되지 않았습니다.");
                return;
            }
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (entity instanceof Player) {
                    entity.teleport(player);
                }
            }
        }

        private void sleepTarget(AbilityPlayerContext context, Player player) {
            Player target = targetPlayer();
            if (target != null && !sameTeam(context, player, target) && player.getLocation().distanceSquared(target.getLocation()) <= 400.0D) {
                effect(target, PotionEffectType.BLINDNESS, 30, 0);
                effect(target, PotionEffectType.SLOW, 30, 3);
            }
        }

        private void teamBuff(AbilityPlayerContext context, Player player) {
            for (Player target : nearbyPlayers(context, player, 20, true)) {
                effect(target, PotionEffectType.SPEED, 15, 0);
                effect(target, PotionEffectType.REGENERATION, 15, 0);
            }
            effect(player, PotionEffectType.SPEED, 15, 0);
            effect(player, PotionEffectType.REGENERATION, 15, 0);
        }

        private void recall(final AbilityPlayerContext context, final Player player) {
            final Location location = player.getLocation();
            later(context, 10, new Runnable() {
                @Override
                public void run() {
                    player.teleport(location);
                    effect(player, PotionEffectType.INVISIBILITY, 3, 0);
                }
            });
        }

        private void iceSphere(final AbilityPlayerContext context, Location center, int radius, int seconds) {
            final Map<Location, Material> oldBlocks = new LinkedHashMap<Location, Material>();
            for (Location location : sphere(center, radius)) {
                Block block = location.getBlock();
                if (block.getType() != Material.DIAMOND_BLOCK) {
                    oldBlocks.put(block.getLocation(), block.getType());
                    block.setType(Material.ICE);
                }
            }
            later(context, seconds, new Runnable() {
                @Override
                public void run() {
                    for (Map.Entry<Location, Material> entry : oldBlocks.entrySet()) {
                        entry.getKey().getBlock().setType(entry.getValue());
                    }
                }
            });
        }

        private List<Location> sphere(Location center, int radius) {
            List<Location> locations = new ArrayList<Location>();
            int bx = center.getBlockX();
            int by = center.getBlockY();
            int bz = center.getBlockZ();
            for (int x = bx - radius; x <= bx + radius; x++) {
                for (int y = by - radius; y <= by + radius; y++) {
                    for (int z = bz - radius; z <= bz + radius; z++) {
                        double distance = (bx - x) * (bx - x) + (by - y) * (by - y) + (bz - z) * (bz - z);
                        if (distance < radius * radius && distance >= (radius - 1) * (radius - 1)) {
                            locations.add(new Location(center.getWorld(), x, y, z));
                        }
                    }
                }
            }
            return locations;
        }

        private void teleportToTargetBlock(Player player) {
            Block block = targetBlock(player, 25);
            Location location = block.getLocation().add(0.5D, 1.0D, 0.5D);
            if (location.getBlock().getType() == Material.AIR && location.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) {
                location.setPitch(player.getLocation().getPitch());
                location.setYaw(player.getLocation().getYaw());
                player.teleport(location);
            }
        }

        private void swapTarget(AbilityPlayerContext context, Player player) {
            Player target = targetPlayer();
            if (target != null && sameTeam(context, player, target)) {
                Location first = player.getLocation();
                Location second = target.getLocation();
                player.teleport(second);
                target.teleport(first);
            }
        }

        private void push(Player player, List<Player> targets, double power) {
            Vector vector = player.getEyeLocation().getDirection().setY(0.5D).normalize().multiply(power);
            for (Player target : targets) {
                target.setVelocity(vector);
            }
        }

        private void dash(Player player) {
            Vector vector = player.getEyeLocation().getDirection();
            vector.setY(0.5D);
            player.setVelocity(vector);
            player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 1);
        }

        private void backstab(AbilityPlayerContext context, Player player) {
            for (Player target : nearbyPlayers(context, player, 10, false)) {
                Location location = target.getLocation().subtract(target.getLocation().getDirection().normalize());
                player.teleport(location);
                return;
            }
            player.sendMessage("스킬을 사용 할 수 있는 상대가 없습니다.");
        }

        private void judgment(AbilityPlayerContext context, final Player player) {
            final List<Player> targets = nearbyPlayers(context, player, 5, false);
            if (targets.isEmpty()) {
                player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
                return;
            }
            player.setHealth(Math.max(1.0D, player.getHealth() / 2.0D));
            for (Player target : targets) {
                target.setVelocity(new Vector(0, 1.6D, 0));
            }
            later(context, 1, new Runnable() {
                @Override
                public void run() {
                    for (Player target : targets) {
                        target.getWorld().strikeLightning(target.getLocation());
                    }
                }
            });
        }

        private void enchantTable(AbilityPlayerContext context, Player player) {
            if (enchantTables <= 0) {
                player.sendMessage("이 능력은 더이상 사용할 수 없습니다.");
                return;
            }
            if (use(context, player, 2, COBBLESTONE, 64, enchantTables > 1 ? 3 : 0)) {
                enchantTables--;
                give(player, Material.ENCHANTMENT_TABLE, 1);
                player.sendMessage("남은 교환 횟수 : " + enchantTables);
            }
        }

        private void bless(Player player) {
            if (RANDOM.nextBoolean()) effect(player, PotionEffectType.DAMAGE_RESISTANCE, 30, 0);
            if (RANDOM.nextBoolean()) effect(player, PotionEffectType.INCREASE_DAMAGE, 30, 0);
            if (RANDOM.nextBoolean()) effect(player, PotionEffectType.REGENERATION, 30, 0);
            if (RANDOM.nextBoolean()) effect(player, PotionEffectType.SPEED, 30, 0);
            if (RANDOM.nextBoolean()) effect(player, PotionEffectType.FAST_DIGGING, 30, 0);
        }

        private void curse(List<Player> players) {
            for (Player player : players) {
                curse(player);
            }
        }

        private void curse(Player player) {
            effect(player, PotionEffectType.HUNGER, 10, 0);
            effect(player, PotionEffectType.POISON, 10, 0);
            effect(player, PotionEffectType.SLOW, 10, 0);
            effect(player, PotionEffectType.SLOW_DIGGING, 10, 0);
        }

        private void pullTarget(AbilityPlayerContext context, Player player, int range) {
            Player target = targetPlayer();
            if (target != null && !sameTeam(context, player, target) && target.getLocation().distanceSquared(player.getLocation()) <= range * range) {
                target.teleport(player);
            }
        }

        private void giveSnow(Player player, AbilityPlayerContext context) {
            if (use(context, player, 0, COBBLESTONE, 1, 0)) {
                give(player, Material.SNOW_BALL, 1);
            }
        }

        private void stealSword(Player player) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && isSword(item.getType())) {
                    tajjaDamage = swordDamage(item.getType());
                    tajjaUses = 10;
                    player.getInventory().removeItem(new ItemStack(item.getType(), 1));
                    player.sendMessage("손은 눈보다 빠르다.");
                    return;
                }
            }
            player.sendMessage("소비할 검이 인벤토리에 없습니다.");
        }

        private void explosionMagic(AbilityPlayerContext context, final Player player) {
            if (oneTimeUsed || !use(context, player, 0, COBBLESTONE, 25, 0)) {
                return;
            }
            oneTimeUsed = true;
            final Block block = targetBlock(player, 25);
            player.sendMessage(ChatColor.RED + "익스플로전!");
            later(context, 3, new Runnable() {
                @Override
                public void run() {
                    player.getWorld().createExplosion(block.getLocation(), 5.0F);
                    player.setHealth(0.0D);
                }
            });
        }

        private void nasdaq(Player player) {
            if (!readyCooldown(player, 0, 30) || !has(player, COBBLESTONE, 20)) {
                return;
            }
            ItemStack item = player.getItemInHand();
            player.getInventory().removeItem(new ItemStack(COBBLESTONE, 20));
            cooldowns.put(0, System.currentTimeMillis() + 30000L);
            if (RANDOM.nextInt(4) < 3) {
                player.getInventory().addItem(item.clone());
            } else {
                player.getInventory().removeItem(item.clone());
            }
        }

        private void sniperReady(final Player player) {
            if (player.isSneaking() && !ready) {
                ready = true;
                player.sendMessage("스나이핑 모드를 준비합니다.");
                Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("NewGodWar"), new Runnable() {
                    @Override
                    public void run() {
                        if (ready) {
                            player.sendMessage("스나이핑 모드가 활성화되었습니다.");
                        }
                    }
                }, 80L);
            }
        }

        private void askQuestion(Player player) {
            String[] questions = new String[] {
                "15*12의 값을 구하시오.", "2+2*2+1의 값을 구하시오.", "가로 4, 세로 3인 직사각형의 넓이를 구하시오."
            };
            int[] answers = new int[] {180, 7, 12};
            int index = RANDOM.nextInt(questions.length);
            pendingQuestion = questions[index];
            pendingAnswer = answers[index];
            player.sendMessage(pendingQuestion);
        }

        private void answerQuestion(AbilityPlayerContext context, AsyncPlayerChatEvent event) {
            if (pendingAnswer < 0) {
                return;
            }
            try {
                int answer = Integer.parseInt(event.getMessage());
                if (answer == pendingAnswer) {
                    context.plugin().abilities().assignRandom(context.player());
                    context.player().sendMessage(ChatColor.AQUA + "문제를 맞혀 새 능력을 얻었습니다!");
                } else {
                    context.player().sendMessage("아쉽습니다! 정답은 " + pendingAnswer + "입니다.");
                }
                pendingAnswer = -1;
                pendingQuestion = null;
            } catch (NumberFormatException ex) {
                context.player().sendMessage("0~999의 음이 아닌 정수만 입력하십시오.");
            }
        }

        private void giveSpellBook(Player player, boolean harry) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) book.getItemMeta();
            meta.setTitle("마법 스펠 암기장");
            meta.setAuthor(harry ? "해리 포터" : "헤르미온느 진 그레인저");
            meta.addPage("루모스/Lumos\n녹스/Nox\n봄바르다/Bombarda");
            meta.addPage("스투페파이/Stupefy\n익스펙토 패트로눔/Expecto Patronum");
            meta.addPage("엑스펠리아무스/Expelliarmus\n아바다 케다브라/Avada Kedavra");
            book.setItemMeta(meta);
            player.getInventory().addItem(book);
        }

        private boolean isPickaxe(Material material) {
            return material == Material.WOOD_PICKAXE || material == Material.STONE_PICKAXE || material == Material.IRON_PICKAXE || material == Material.DIAMOND_PICKAXE;
        }

        private boolean isSword(Material material) {
            return material == Material.WOOD_SWORD || material == Material.GOLD_SWORD || material == Material.STONE_SWORD || material == Material.IRON_SWORD || material == Material.DIAMOND_SWORD;
        }

        private int swordDamage(Material material) {
            if (material == Material.WOOD_SWORD || material == Material.GOLD_SWORD) return 4;
            if (material == Material.STONE_SWORD) return 5;
            if (material == Material.IRON_SWORD) return 6;
            if (material == Material.DIAMOND_SWORD) return 7;
            return 1;
        }
    }
}
