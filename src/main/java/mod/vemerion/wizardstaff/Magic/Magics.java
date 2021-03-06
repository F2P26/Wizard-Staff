package mod.vemerion.wizardstaff.Magic;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import mod.vemerion.wizardstaff.Main;
import mod.vemerion.wizardstaff.Magic.fashionupdate.FashionMagic;
import mod.vemerion.wizardstaff.Magic.netherupdate.GhastTearMagic;
import mod.vemerion.wizardstaff.Magic.netherupdate.GlowstoneDustMagic;
import mod.vemerion.wizardstaff.Magic.netherupdate.GoldNuggetMagic;
import mod.vemerion.wizardstaff.Magic.netherupdate.LodestoneMagic;
import mod.vemerion.wizardstaff.Magic.netherupdate.NetherBrickMagic;
import mod.vemerion.wizardstaff.Magic.netherupdate.NetheriteIngotMagic;
import mod.vemerion.wizardstaff.Magic.netherupdate.NetherrackMagic;
import mod.vemerion.wizardstaff.Magic.netherupdate.ObsidianMagic;
import mod.vemerion.wizardstaff.Magic.netherupdate.SoulSandMagic;
import mod.vemerion.wizardstaff.Magic.netherupdate.WitherSkullMagic;
import mod.vemerion.wizardstaff.Magic.original.BlazePowderMagic;
import mod.vemerion.wizardstaff.Magic.original.CarvedPumpkinMagic;
import mod.vemerion.wizardstaff.Magic.original.ClockMagic;
import mod.vemerion.wizardstaff.Magic.original.EggMagic;
import mod.vemerion.wizardstaff.Magic.original.ElytraMagic;
import mod.vemerion.wizardstaff.Magic.original.GoldMagic;
import mod.vemerion.wizardstaff.Magic.original.JukeboxMagic;
import mod.vemerion.wizardstaff.Magic.original.WizardStaffMagic;
import mod.vemerion.wizardstaff.Magic.original.WritableBookMagic;
import mod.vemerion.wizardstaff.Magic.suggestions.BlueDyeMagic;
import mod.vemerion.wizardstaff.Magic.suggestions.BricksMagic;
import mod.vemerion.wizardstaff.Magic.suggestions.FeatherMagic;
import mod.vemerion.wizardstaff.Magic.suggestions.GrapplingHookMagic;
import mod.vemerion.wizardstaff.Magic.suggestions.MushroomCloudMagic;
import mod.vemerion.wizardstaff.Magic.suggestions.ShulkerBulletMagic;
import mod.vemerion.wizardstaff.Magic.suggestions.WaterBucketMagic;
import mod.vemerion.wizardstaff.network.Network;
import mod.vemerion.wizardstaff.network.UpdateMagicsMessage;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class Magics extends JsonReloadListener {
	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	private static final String FOLDER_NAME = Main.MODID + "-magics";

	private static Magics instance;

	private Map<String, Supplier<Magic>> magicNames;
	private Map<ResourceLocation, Magic> magics;

	// Save all the magic params on server to send to client at login
	private Map<ResourceLocation, MagicParams> magicParams;
	private Map<Item, ResourceLocation> cache;
	private final NoMagic NO_MAGIC = new NoMagic();

	private Magics() {
		super(GSON, FOLDER_NAME);
		this.magicNames = new HashMap<>();
		this.magics = new HashMap<>();
		this.cache = new HashMap<>();
		this.magicParams = new HashMap<>();
		this.initMagicNames();
	}

	public Magic get(ItemStack stack) {
		Item item = stack.getItem();
		if (cache.containsKey(item) && magics.get(cache.get(item)).isMagicItem(stack)) {
			return magics.get(cache.get(item));
		} else {
			for (Entry<ResourceLocation, Magic> entry : magics.entrySet()) {
				if (entry.getValue().isMagicItem(stack)) {
					Magic magic = entry.getValue();
					cache.put(stack.getItem(), entry.getKey());
					return magic;
				}
			}
		}
		return NO_MAGIC;
	}

	private void initMagicNames() {
		magicNames.put("blaze_powder_magic", () -> new BlazePowderMagic());
		magicNames.put("carved_pumpkin_magic", () -> new CarvedPumpkinMagic());
		magicNames.put("clock_magic", () -> new ClockMagic());
		magicNames.put("egg_magic", () -> new EggMagic());
		magicNames.put("elytra_magic", () -> new ElytraMagic());
		magicNames.put("gold_magic", () -> new GoldMagic());
		magicNames.put("jukebox_magic", () -> new JukeboxMagic());
		magicNames.put("wizard_staff_magic", () -> new WizardStaffMagic());
		magicNames.put("writable_book_magic", () -> new WritableBookMagic());
		magicNames.put("obsidian_magic", () -> new ObsidianMagic());
		magicNames.put("glowstone_dust_magic", () -> new GlowstoneDustMagic());
		magicNames.put("netherrack_magic", () -> new NetherrackMagic());
		magicNames.put("wither_skull_magic", () -> new WitherSkullMagic());
		magicNames.put("ghast_tear_magic", () -> new GhastTearMagic());
		magicNames.put("nether_brick_magic", () -> new NetherBrickMagic());
		magicNames.put("soul_sand_magic", () -> new SoulSandMagic());
		magicNames.put("gold_nugget_magic", () -> new GoldNuggetMagic());
		magicNames.put("lodestone_magic", () -> new LodestoneMagic());
		magicNames.put("netherite_ingot_magic", () -> new NetheriteIngotMagic());
		magicNames.put("wizard_boots_fashion_magic", () -> new FashionMagic(Main.WIZARD_BOOTS_ITEM));
		magicNames.put("wizard_chestplate_fashion_magic", () -> new FashionMagic(Main.WIZARD_CHESTPLATE_ITEM));
		magicNames.put("wizard_helmet_fashion_magic", () -> new FashionMagic(Main.WIZARD_HAT_ITEM));
		magicNames.put("wizard_leggings_fashion_magic", () -> new FashionMagic(Main.WIZARD_LEGGINGS_ITEM));
		magicNames.put("druid_boots_fashion_magic", () -> new FashionMagic(Main.DRUID_BOOTS_ITEM));
		magicNames.put("druid_chestplate_fashion_magic", () -> new FashionMagic(Main.DRUID_CHESTPLATE_ITEM));
		magicNames.put("druid_helmet_fashion_magic", () -> new FashionMagic(Main.DRUID_HELMET_ITEM));
		magicNames.put("druid_leggings_fashion_magic", () -> new FashionMagic(Main.DRUID_LEGGINGS_ITEM));
		magicNames.put("warlock_boots_fashion_magic", () -> new FashionMagic(Main.WARLOCK_BOOTS_ITEM));
		magicNames.put("warlock_chestplate_fashion_magic", () -> new FashionMagic(Main.WARLOCK_CHESTPLATE_ITEM));
		magicNames.put("warlock_helmet_fashion_magic", () -> new FashionMagic(Main.WARLOCK_HELMET_ITEM));
		magicNames.put("warlock_leggings_fashion_magic", () -> new FashionMagic(Main.WARLOCK_LEGGINGS_ITEM));
		magicNames.put("blue_dye_magic", () -> new BlueDyeMagic());
		magicNames.put("bricks_magic", () -> new BricksMagic());
		magicNames.put("grappling_hook_magic", () -> new GrapplingHookMagic());
		magicNames.put("feather_magic", () -> new FeatherMagic());
		magicNames.put("mushroom_cloud_magic", () -> new MushroomCloudMagic());
		magicNames.put("shulker_bullet_magic", () -> new ShulkerBulletMagic());
		magicNames.put("water_bucket_magic", () -> new WaterBucketMagic());
		magicNames.put("no_magic", () -> NO_MAGIC);
	}

	public static Magics getInstance() {
		return instance;
	}

	public static void init() {
		instance = new Magics();
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> objectIn, IResourceManager resourceManagerIn,
			IProfiler profilerIn) {
		Map<ResourceLocation, MagicParams> params = new HashMap<>();
		for (Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
			JsonObject json = JSONUtils.getJsonObject(entry.getValue(), "top element");
			float cost = JSONUtils.getFloat(json, "cost");
			if (cost < 0)
				throw new JsonSyntaxException("The cost of a magic can not be negative");
			int duration = JSONUtils.getInt(json, "duration");
			String magic = JSONUtils.getString(json, "magic");
			if (!magicNames.containsKey(magic))
				throw new JsonSyntaxException("The magic " + magic + " does not exist");
			Ingredient ingredient = Ingredient.deserialize(json.get("ingredient"));
			params.put(entry.getKey(), new MagicParams(cost, duration, magic, ingredient));
		}

		magicParams.putAll(params);
		addMagics(params);
		if (ServerLifecycleHooks.getCurrentServer() != null)
			sendMagicMessage(params);
	}

	private void sendMagicMessage(Map<ResourceLocation, MagicParams> params) {
		Network.INSTANCE.send(PacketDistributor.ALL.noArg(), new UpdateMagicsMessage(params));
	}

	public void sendAllMagicMessage(ServerPlayerEntity reciever) {
		Network.INSTANCE.send(PacketDistributor.PLAYER.with(() -> reciever), new UpdateMagicsMessage(magicParams));
	}

	public void addMagics(Map<ResourceLocation, MagicParams> params) {
		cache = new HashMap<>();
		for (Entry<ResourceLocation, MagicParams> entry : params.entrySet()) {
			magics.put(entry.getKey(), entry.getValue().createMagic());
		}
	}

	public static class MagicParams {
		private float cost;
		private int duration;
		private String magicKey;
		private Ingredient ingredient;

		public MagicParams(float cost, int duration, String magicKey, Ingredient ingredient) {
			this.cost = cost;
			this.duration = duration;
			this.magicKey = magicKey;
			this.ingredient = ingredient;
		}

		public Magic createMagic() {
			Magic magic = getInstance().magicNames.get(magicKey).get();
			magic.init(cost, duration < 0 ? Magic.HOUR : duration, ingredient);
			return magic;
		}

		public static MagicParams decode(PacketBuffer buffer) {
			return new MagicParams(buffer.readFloat(), buffer.readInt(), buffer.readString(100),
					Ingredient.read(buffer));
		}

		public void encode(PacketBuffer buffer) {
			buffer.writeFloat(cost);
			buffer.writeInt(duration);
			buffer.writeString(magicKey);
			ingredient.write(buffer);
		}
	}
}
