package me.finnbueno.waterRing.consumption;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.configuration.Config;
import com.projectkorra.projectkorra.waterbending.SurgeWall;
import com.projectkorra.projectkorra.waterbending.SurgeWave;
import com.projectkorra.projectkorra.waterbending.Torrent;
import com.projectkorra.projectkorra.waterbending.WaterManipulation;
import commonslang3.projectkorra.lang3.StringUtils;
import me.finnbueno.waterRing.utils.IAbilityFinder;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ConsumptionConfigurationManager {

    public static final String CONFIG_SECTION_PATH = "ExtraAbilities.FinnBueno.WaterRing.Consumption";

    private static final Map<String, Object> DEFAULT_TORRENT_VALUES = createDefaultConfigValueMap("Torrent", Torrent.class, 5, false);
    private static final Map<String, Object> DEFAULT_WATER_MANIPULATION_VALUES = createDefaultConfigValueMap("WaterManipulation", WaterManipulation.class, 1, false);
    private static final Map<String, Object> DEFAULT_SURGE_WALL_VALUES = createDefaultConfigValueMap("Surge", SurgeWall.class, 5, true);
    private static final Map<String, Object> DEFAULT_SURGE_WAVE_VALUES = createDefaultConfigValueMap("Surge", SurgeWave.class, 5, false);

    private static final String NAME_PATH = "Name";
    private static final String CLASSNAME_PATH = "ClassName";
    private static final String USES_PATH = "Uses";
    private static final String REFUNDABLE_PATH = "Refundable";

    private final Map<String, ConsumptionConfiguration> consumptionConfiguration = new HashMap<>();

    private final FileConfiguration fileConfiguration;
    private final Config config;
    private final IAbilityFinder abilityFinder;

    public ConsumptionConfigurationManager(Config config, IAbilityFinder abilityFinder) {
        this.fileConfiguration = config.get();
        this.config = config;
        this.abilityFinder = abilityFinder;
    }

    public ConsumptionConfiguration getConfiguration(Class<? extends WaterAbility> waterAbilityClass) {
        return consumptionConfiguration.get(waterAbilityClass.getSimpleName());
    }

    private static Map<String, Object> createDefaultConfigValueMap(String name, Class<? extends WaterAbility> abilityClass, int uses, boolean isRefundable) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Name", name);
        map.put("Uses", uses);
        map.put("Refundable", isRefundable);
        if (abilityClass != null) {
            map.put("ClassName", abilityClass.getSimpleName());
        }
        return map;
    }

    public void addDefaults() {
        fileConfiguration.addDefault(CONFIG_SECTION_PATH, List.of(
            DEFAULT_TORRENT_VALUES,
            DEFAULT_WATER_MANIPULATION_VALUES,
            DEFAULT_SURGE_WALL_VALUES,
            DEFAULT_SURGE_WAVE_VALUES
        ));
        config.save();
    }

    public void load() {
        List<Map<String, Object>> entryList = (List<Map<String, Object>>) fileConfiguration.getList(CONFIG_SECTION_PATH);
        if (entryList == null) {
            throw new NullPointerException("The config section for WaterRing's ammunition consumption is missing or not a list");
        }

        Map<Integer, Set<String>> configErrors = new HashMap<>();
        for (int i = 0; i < entryList.size(); i++) {
            Map<String, Object> keyValuesForEntry = entryList.get(i);
            Set<String> errors = verifyEntryValidity(keyValuesForEntry);

            if (!errors.isEmpty()) {
                configErrors.put(i, errors);
                continue;
            }

            int uses = (Integer) keyValuesForEntry.get(USES_PATH);
            boolean isRefundable = (Boolean) keyValuesForEntry.get(REFUNDABLE_PATH);
            String className = (String) keyValuesForEntry.get(CLASSNAME_PATH);
            String abilityName = (String) keyValuesForEntry.get(NAME_PATH);

            Method getSourceBlockMethod;
            CoreAbility abilityInstance;
            try {
                abilityInstance = getCoreAbilityByNameOrClass(className, abilityName);
                getSourceBlockMethod = getSourceGetterMethod(abilityInstance, className, abilityName);
            } catch (IllegalArgumentException e) {
                configErrors.put(i, Set.of(e.getMessage()));
                continue;
            }

            consumptionConfiguration.put(
                    abilityInstance.getClass().getSimpleName(),
                    new ReflectiveConsumptionConfiguration(abilityInstance.getName(), getSourceBlockMethod, uses, isRefundable));
        }

        if (!configErrors.isEmpty()) {
            configErrors.forEach((entry, errors) -> {
                throw new IllegalArgumentException(
                        "Encountered config errors for WaterRing consumption entry %s (%s)".formatted(
                                entry,
                                StringUtils.join(errors, ", ")
                        )
                );
            });
        }
    }

    private CoreAbility getCoreAbilityByNameOrClass(String className, String abilityName) throws IllegalArgumentException {
        CoreAbility abilityInstance;
        if (className != null) {
            abilityInstance = abilityFinder.getAbilityByClassName(className);
            if (abilityInstance == null) {
                throw new IllegalArgumentException("Could not find an ability by classname %s".formatted(className));
            }
        } else {
            abilityInstance = abilityFinder.getAbilityByName(abilityName);
            if (abilityInstance == null) {
                throw new IllegalArgumentException("Could not find a class for ability by name %s".formatted(abilityName));
            }
        }
        return abilityInstance;
    }

    private Method getSourceGetterMethod(CoreAbility abilityInstance, String className, String abilityName) throws IllegalArgumentException {
        Class<? extends CoreAbility> abilityClass = abilityInstance.getClass();

        String abilitySpecificationTerm = className == null ? "name" : "classname";
        String abilityNameOrClassName = className == null ? abilityName : className;
        Method getSourceBlockMethod;
        try {
            getSourceBlockMethod = abilityClass.getDeclaredMethod("getSourceBlock");
            getSourceBlockMethod.setAccessible(true);
            if (getSourceBlockMethod.getReturnType() != Block.class) {
                throw new IllegalArgumentException(
                        "getSourceBlock method for %s %s does not return org.bukkit.block.Block".formatted(
                                abilitySpecificationTerm,
                                abilityNameOrClassName));
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Ability by %s %s does not have a valid getSourceBlock method".formatted(
                            abilitySpecificationTerm,
                            abilityNameOrClassName));
        }

        return getSourceBlockMethod;
    }

    private Set<String> verifyEntryValidity(Map<String, Object> keyValues) {
        Set<String> errors = new HashSet<>();

        if (keyValues.get(NAME_PATH) instanceof String name) {
            if (name.isEmpty()) {
                errors.add("'Name' cannot be an empty string");
            }
        } else {
            errors.add("'Name' must be specified");
        }

        Object usesAsObject = keyValues.get(USES_PATH);
        if (usesAsObject instanceof Integer uses) {
            if (uses < -1) {
                errors.add("'Uses' must be an integer of -1 or larger");
            }
        } else {
            errors.add("'Uses' must be an integer of -1 or larger");
        }

        boolean isRefundableSpecified = keyValues.get(REFUNDABLE_PATH) instanceof Boolean;
        if (!isRefundableSpecified) {
            errors.add("'Refundable' must be true or false");
        }
        return errors;
    }

    public String getConfiguredMovesAsStringList() {
        List<String> listOfMoves = consumptionConfiguration.values().stream()
                .map(ConsumptionConfiguration::getAbilityName)
                .distinct()
                .toList();
        if (listOfMoves.isEmpty()) {
            return "none";
        }
        return String.join(", ", listOfMoves.subList(0, listOfMoves.size() - 1)) + " and " + listOfMoves.getLast();
    }
}
