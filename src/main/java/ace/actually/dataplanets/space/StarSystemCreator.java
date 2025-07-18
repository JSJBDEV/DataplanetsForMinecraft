package ace.actually.dataplanets.space;

import ace.actually.dataplanets.compat.Compat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class StarSystemCreator {

    public static void makeSystem()
    {
        String uuid = UUID.randomUUID().toString();
        inventSystem(uuid);
    }

    private static String truth(boolean truth)
    {
        if(truth) return "true";
        return "false";
    }

    private static final String[] CODE = new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};

    private static void inventSystem(String uuid)
    {
        RandomSource random = RandomSource.create();
        String systemName = CODE[random.nextInt(CODE.length)]+CODE[random.nextInt(CODE.length)]+random.nextInt(1000);
        int rocketTier = random.nextInt(3)+1;


        CompoundTag systemData = new CompoundTag();
        systemData.putString("systemName",systemName);
        systemData.putInt("rocketTier",rocketTier);
        systemData.putInt("planets",random.nextInt(0,6));



        StringBuilder langFile = new StringBuilder("{\"dataplanets.").append(systemName).append("\":\"");
        String rebuild = systemName.toUpperCase();
        rebuild = rebuild.substring(0,2)+" "+rebuild.substring(2);
        langFile.append(rebuild).append("\",");

        for (int i = 0; i < systemData.getInt("planets"); i++) {
            CompoundTag planetData = new CompoundTag();
            String subname = CODE[i];
            String planetName = systemName+subname;
            planetData.putString("name",planetName);
            planetData.putFloat("gravity",(float)random.nextInt(1500)/100f);
            planetData.putBoolean("hasAtmosphere",random.nextInt(4)==0);
            planetData.putInt("yearDays",random.nextInt(1,1000));


            int initalTemperature = random.nextInt(1000);

            //a planet with an atmosphere is more likely to have a reasonable temperature
            if(planetData.getBoolean("hasAtmosphere") && initalTemperature<100)
            {
                initalTemperature+=100;
            }
            if(random.nextInt(10)==0)
            {
                planetData.putString("planetType","gaseous");
            }

            //temperature limits solar power - that's wierd right?
            //a higher solar power would imply a higher temperature from the sun.
            //(this is a massive simplification when you consider different radiation types...)
            //however higher temperatures can be generated in a planet without its star (tidal flexing/heating, volcanic activity etc.)
            //hence, as temperature isn't necesarily dependent on the star, its this way round
            //MAYBE: does this make sense?
            planetData.putInt("temperature",initalTemperature);

            //solar power 50 is REALLY high, Mercury has 19 at 440K
            //however, the planet needs a temeprature of 1000 to get 49 (upper bounds)
            //that's rather warm.
            //nicely, a planet with temperature 401 has a chance of getting 20 (still 1-20 however)
            planetData.putInt("solarPower",random.nextInt((planetData.getInt("temperature")/15)+1));

            planetData.putBoolean("hasOxygen",random.nextInt(4)==0);

            String effects;
            if(planetData.getBoolean("hasAtmosphere"))
            {
                effects="minecraft:overworld";
            }
            else
            {
                effects="minecraft:the_end";
            }

            planetData.putString("effects",effects);


            //TODO: multiple biomes per planet

            planetData.putInt("skyColour",random.nextInt(16777215));
            planetData.putInt("fogColour",random.nextInt(16777215));
            planetData.putInt("waterColour",random.nextInt(16777215));
            planetData.putInt("waterFogColour",random.nextInt(16777215));
            planetData.putInt("grassColour",random.nextInt(16777215));
            planetData.putInt("foliageColour",random.nextInt(16777215));
            planetData.putInt("seaLevel",random.nextInt(63,210));
            planetData.putString("generalBlock", Compat.SURFACE_BLOCKS[random.nextInt(Compat.SURFACE_BLOCKS.length)]);

            if(planetData.getBoolean("hasAtmosphere") && random.nextInt(5)==0)
            {
                if(planetData.getInt("temperature")<373)
                {
                    if(planetData.getInt("temperature")>273)
                    {
                        planetData.putString("seaBlock","minecraft:water");
                        planetData.putString("planetType","ocean");
                    }
                    else
                    {
                        planetData.putString("seaBlock","minecraft:packed_ice");
                        planetData.putString("planetType","icy");
                    }

                }
                else
                {
                    planetData.putString("seaBlock","minecraft:air");
                }

            }
            else
            {
                planetData.putString("seaBlock","minecraft:air");
            }

            byte[] flavour = new byte[20];
            for(int z=0; z<flavour.length; z++)
            {
                if(random.nextBoolean())
                {
                    flavour[z]=1;
                }
                else
                {
                    flavour[z]=0;
                }
            }
            planetData.putByteArray("flavour",flavour);

            shouldGenDripstone(planetData,random);
            genLakes(planetData,uuid,random);
            genRocks(planetData,uuid,random);
            genOre(random,uuid,planetData);


            planetData.putFloat("nr1", (float) random.nextInt(1800) /1000f);
            planetData.putFloat("nr2", (float) random.nextInt(2000) /1000f);
            planetData.putFloat("nr3", (float) random.nextInt(2000) /1000f);


            planetData.putFloat("radiusClient",(((50f-planetData.getInt("solarPower")) /10f)+random.nextFloat()));
            planetData.putInt("scaleClient",random.nextInt(5,20));

            langFile.append("\"level.").append(planetName).append("\":\"");
            rebuild = planetName.toUpperCase();
            rebuild = rebuild.substring(0,2)+" "+rebuild.substring(2,rebuild.length()-1)+" "+rebuild.toLowerCase().charAt(rebuild.length()-1);
            langFile.append(rebuild).append("\",");


            systemData.put(planetName,planetData);
        }
        CompoundTag tag = getDynamicDataOrNew();
        tag.put(systemName,systemData);
        writeToDynamic(tag);


    }

    public static CompoundTag getDynamicDataOrNew()
    {
        CompoundTag tag;
        File storage = new File("./dataplanets_dynamic_data.dat");
        if(storage.exists())
        {
            try {
                tag = NbtIo.readCompressed(new File("./dataplanets_dynamic_data.dat"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else
        {
            tag = new CompoundTag();
        }
        return tag;
    }
    private static void writeToDynamic(CompoundTag tag)
    {
        File storage = new File("./dataplanets_dynamic_data.dat");
        try {
            NbtIo.writeCompressed(tag,storage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String genOre(RandomSource random,String uuid,CompoundTag planetData)
    {

        ResourceLocation[] candidate = BuiltInRegistries.BLOCK.keySet().stream().filter(a->a.getPath().contains("_ore")).toArray(ResourceLocation[]::new);
        ResourceLocation orerl = candidate[random.nextInt(candidate.length)];
        String ore = orerl.toString();
        ListTag ores;
        if(planetData.contains("planet_ores"))
        {
            ores = (ListTag) planetData.get("planet_ores");
        }
        else
        {
            ores = new ListTag();
        }
        ores.add(StringTag.valueOf(ore));
        planetData.put("planet_ores",ores);
        String reform = ore.replace(":","_");

        return ",\""+"dataplanets:ORE\"".replace("ORE",reform);
    }



    private static String shouldGenDripstone(CompoundTag planetData,RandomSource source)
    {
        //\"minecraft:large_dripstone\",\n" +
        //                    "      \"minecraft:dripstone_cluster\",\n" +
        //                    "      \"minecraft:pointed_dripstone\"\n" +
        StringBuilder stringBuilder = new StringBuilder();
        if(source.nextInt(5)==0)
        {
            stringBuilder.append("\"minecraft:dripstone_cluster\",\n");

        }
        if(source.nextInt(5)==0)
        {
            stringBuilder.append("\"minecraft:large_dripstone\",\n");
        }
        if(source.nextInt(5)==0)
        {
            stringBuilder.append("\"minecraft:pointed_dripstone\",\n");
        }
        String v = stringBuilder.toString();

        if(v.isEmpty()) return "";
        return v.substring(0,v.length()-1);
    }



    private static String genLakes(CompoundTag planetData, String uuid, RandomSource randomSource)
    {
        StringBuilder features = new StringBuilder();


        for (int i = 0; i < 1; i++) {

            List<Fluid> materials = BuiltInRegistries.FLUID.stream().filter(a->
            {
                int temp = a.getFluidType().getTemperature();

                //if the fluid temperature is >300 it's not set as a liquid at room temperature so is *probably* molten.
                //in this case, we need the planet to be warmer than its liquid state
                if(temp<=planetData.getInt("temperature") && temp>300)
                {
                    return true;
                }
                //if the fluid temeperature <301 you probably need to cool it to get it as a liquid, or its liquid at room temperature
                //in this case, we want the planet to be colder than its liquid state
                if(planetData.getInt("temperature")<=temp && temp<301)
                {
                    return true;
                }
                //since we only have 1 number (what temperature it is when it's a liquid) we can't infer when something freezes solid
                //or when something evaporates.

                return false;
            }).toList();

            String matName;
            String fluidName;

            //these first two conditions should only trigger if a planet is really, really hot or really, really cold.
            //and they should only trigger at all if we are in a gamestate where we have no other fluids
            if(materials.isEmpty() && planetData.getInt("temperature")<301)
            {
                fluidName = "minecraft:packed_ice";
            }
            else if(materials.isEmpty() && planetData.getInt("temperature")>300)
            {
                fluidName = "minecraft:lava";
            }
            else
            {
                fluidName = BuiltInRegistries.FLUID.getKey(materials.get(randomSource.nextInt(materials.size()))).toString();
            }

            ListTag lakes;
            if(planetData.contains("lakeFluids"))
            {
                lakes = (ListTag) planetData.get("lakeFluids");
            }
            else
            {
                lakes = new ListTag();
            }
            lakes.add(StringTag.valueOf(fluidName));

            planetData.put("lakeFluids",lakes);

        }



        return features.toString();
    }
    private static String genRocks(CompoundTag planetData, String uuid, RandomSource randomSource)
    {
        StringBuilder features = new StringBuilder();
        String matName = Compat.SURFACE_BLOCKS[randomSource.nextInt(Compat.SURFACE_BLOCKS.length)];

        ListTag rocks;
        if(planetData.contains("rock_blocks"))
        {
            rocks = (ListTag) planetData.get("rock_blocks");
        }
        else
        {
            rocks = new ListTag();
        }
        rocks.add(StringTag.valueOf(matName));


        planetData.put("rock_blocks",rocks);



        return features.toString();
    }

    static boolean writeCond = false;
    private static void writeLinesCond(File file, List<String> strings) throws IOException {
        if(writeCond)
        {
            FileUtils.writeLines(file,strings);
        }
    }
}
