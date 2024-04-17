package net.teefoss.dnicraft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;

import java.util.*;

public class Age {
    public enum Terrain {
        NORMAL,
        CAVERNOUS,
        ERODED,
    }

    public enum Extent {
        EXTREMELY_LOW,
        VERY_LOW,
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH,
        EXTREMELY_HIGH,
    }

    public UUID uuid;

    // DimensionType Parameters
    boolean fixedTime = false;
    long time = 0;
    boolean hasSun = true; // if false, set night, hasSkyLight = false,
    Terrain terrain = Terrain.NORMAL; // Adjust height per terrain type

    // Noise Settings (ChunkGeneratorSettings) Parameters
    BlockState groundBlock = Blocks.STONE.getDefaultState();
    BlockState waterBlock = Blocks.WATER.getDefaultState(); // no oceans = air
    Extent temperature = Extent.MEDIUM; // don't touch 'frozen' biome property
    int oceanLevel = 63;
    boolean floodedCaves = false; // Aquifers enabled

    // Biome Parameters
    boolean hasPrecipitation = true;
    int grassColor = 0xFFFF00FF;
    int skyColor = 0xFF999999;
    int waterColor = 0xFFFF0000;

    // These are a resource location in the form namespace:path
    // When a dimension is loaded, A RegistryEntry<PlacedFeature> can be found from this RL.
    public record Feature(
        String category, // GenerationStep.Feature
        String resourceLocation
    ) {
    }

    List<Feature> features = new ArrayList<>();

    // TODO: control weather, e.g. always raining/snowing

    public Age() {

    }

    public void randomize() {
        Random random = new Random();

        fixedTime = random.nextBoolean();
        time = 0;
        if (fixedTime) {
            time = random.nextLong(0, 24000);
            System.out.printf("fixed time: %d\n", time);
        } else {
            System.out.printf("time cycle\n");
        }
        hasSun = true;

        Terrain[] terrainValues = Terrain.values();
        terrain = terrainValues[random.nextInt(0, terrainValues.length)];
        groundBlock = Blocks.STONE.getDefaultState();
        waterBlock = Blocks.WATER.getDefaultState();

        Extent[] extentValues = Extent.values();
        temperature = extentValues[random.nextInt(0, extentValues.length)];
        oceanLevel = random.nextInt(0, 128);
        floodedCaves = random.nextBoolean();
        hasPrecipitation = random.nextBoolean();
        grassColor = random.nextInt(0, 0x1000000);
        grassColor |= 0xFF000000;
        skyColor = random.nextInt(0, 0x1000000);
        skyColor |= 0xFF000000;
        waterColor = random.nextInt(0, 0x1000000);
        waterColor |= 0xFF000000;
    }

    public Age(NbtCompound tag) {
        System.out.println("init Age from NBT");

        uuid = tag.getUuid("uuid");
        fixedTime = tag.getBoolean("fixedTime");
        if (fixedTime) {
            time = tag.getLong("time");
        }
        hasSun = tag.getBoolean("hasSun");

        Terrain[] terrainValues = Terrain.values();
        terrain = terrainValues[tag.getInt("terrain")];
        groundBlock = Block.getStateFromRawId(tag.getInt("groundBlock"));
        waterBlock = Block.getStateFromRawId(tag.getInt("waterBlock"));
        temperature = Extent.values()[tag.getInt("temperature")];
        oceanLevel = tag.getInt("oceanLevel");
        floodedCaves = tag.getBoolean("floodedCaves");
        hasPrecipitation = tag.getBoolean("hasPrecipitation");
        grassColor = tag.getInt("grassColor");
        skyColor = tag.getInt("skyColor");
        waterColor = tag.getInt("waterColor");

        int numFeatures = tag.getInt("numFeatures");
        for (int i = 0; i < numFeatures; i++) {
            String category = tag.getString("featureCategory" + i);
            String location = tag.getString("featureLocation" + i);
            Feature f = new Feature(category, location);
            features.add(i, f);
        }
    }

    NbtCompound makeTag() {
        NbtCompound tag = new NbtCompound();
        tag.putUuid("uuid", uuid);
        tag.putBoolean("fixedTime", fixedTime);
        if (fixedTime) {
            tag.putLong("time", time);
        }
        tag.putBoolean("hasSun", hasSun);
        tag.putInt("terrain", terrain.ordinal());
        tag.putInt("groundBlock", Block.getRawIdFromState(groundBlock));
        tag.putInt("waterBlock", Block.getRawIdFromState(waterBlock));
        tag.putInt("temperature", temperature.ordinal());
        tag.putInt("oceanLevel", oceanLevel);
        tag.putBoolean("floodedCaves", floodedCaves);
        tag.putBoolean("hasPrecipitation", hasPrecipitation);
        tag.putInt("grassColor", grassColor);
        tag.putInt("skyColor", skyColor);
        tag.putInt("waterColor", waterColor);

        tag.putInt("numFeatures", features.size());
        for (int i = 0; i < features.size(); i++) {
            tag.putString("featureCategory" + i, features.get(i).category);
            tag.putString("featureLocation" + i, features.get(i).resourceLocation);
        }

        return tag;
    }

    float getTemperature() {
        switch (temperature) {
            case EXTREMELY_LOW -> {
                return -2.0f;
            }
            case VERY_LOW -> {
                return -1.33f;
            }
            case LOW -> {
                return -0.66f;
            }
            case MEDIUM -> {
                return 0.0f;
            }
            case HIGH -> {
                return 0.66f;
            }
            case VERY_HIGH -> {
                return 1.33f;
            }
            case EXTREMELY_HIGH -> {
                return 2.0f;
            }
            default -> {
                return 0.0f;
            }
        }
    }
}
