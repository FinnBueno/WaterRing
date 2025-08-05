package me.finnbueno.waterring;

import com.projectkorra.projectkorra.configuration.Config;
import me.finnbueno.waterRing.consumption.ConsumptionConfiguration;
import me.finnbueno.waterRing.consumption.ConsumptionConfigurationManager;
import me.finnbueno.waterRing.utils.IAbilityFinder;
import me.finnbueno.waterring.utils.AbilityWithInvalidSourceGetter;
import me.finnbueno.waterring.utils.AbilityWithValidSourceGetter;
import me.finnbueno.waterring.utils.AbilityWithoutSourceGetter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(value = MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class ConfigurationTests {

    private static final File NON_LIST_VALUE;
    private static final File ONE_ENTRY_MISSING_MULTIPLE_KEYS;
    private static final File ONE_ENTRY_MISSING_NAME;
    private static final File ONE_ENTRY;
    private static final File ONE_ENTRY_WITH_CLASS;
    static {
        ClassLoader classLoader = ConfigurationTests.class.getClassLoader();
        NON_LIST_VALUE = new File(
                Objects.requireNonNull(classLoader.getResource("nonListValue.yml")).getFile());
        ONE_ENTRY_MISSING_MULTIPLE_KEYS = new File(
                Objects.requireNonNull(classLoader.getResource("oneEntryMissingMultipleKeys.yml")).getFile());
        ONE_ENTRY_MISSING_NAME = new File(
                Objects.requireNonNull(classLoader.getResource("oneEntryMissingName.yml")).getFile());
        ONE_ENTRY = new File(
                Objects.requireNonNull(classLoader.getResource("oneEntry.yml")).getFile());
        ONE_ENTRY_WITH_CLASS = new File(
                Objects.requireNonNull(classLoader.getResource("oneEntryWithClass.yml")).getFile());
    }

    @Mock
    private Config config;

    @Mock
    private IAbilityFinder abilityFinder;

    private FileConfiguration fileConfiguration;
    private ConsumptionConfigurationManager consumptionConfigurationManager;

    @BeforeEach
    public void beforeAll() {
        fileConfiguration = new YamlConfiguration();
        Mockito.when(config.get()).thenReturn(fileConfiguration);
        this.consumptionConfigurationManager = new ConsumptionConfigurationManager(config, abilityFinder);
    }

    @Test
    public void givenConfigIsMissing_WhenLoading_ThrowsException() {
        givenConfigContains(null);

        NullPointerException exception = assertThrows(NullPointerException.class, this::whenLoading);
        String expectedMessage = "The config section for WaterRing's ammunition consumption is missing or not a list";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void givenConfigListIsEmpty_WhenLoading_ThrowsException() {
        givenConfigContains(List.of());

        NullPointerException exception = assertThrows(NullPointerException.class, this::whenLoading);
        String expectedMessage = "The config section for WaterRing's ammunition consumption is missing or not a list";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void givenConfigListIsWrongType_WhenLoading_ThrowsException() {
        givenConfigIs(NON_LIST_VALUE);

        NullPointerException exception = assertThrows(NullPointerException.class, this::whenLoading);
        String expectedMessage = "The config section for WaterRing's ammunition consumption is missing or not a list";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void givenConfigEntryMissesMultipleKeys_WhenLoading_ThrowsException() {
        givenConfigIs(ONE_ENTRY_MISSING_MULTIPLE_KEYS);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, this::whenLoading);
        String expectedMessage = "Encountered config errors for WaterRing consumption entry 0 ('Uses' must be an integer of -1 or larger, 'Refundable' must be true or false)";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void givenConfigEntryMissesName_WhenLoading_ThrowsException() {
        givenConfigIs(ONE_ENTRY_MISSING_NAME);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, this::whenLoading);
        String expectedMessage = "Encountered config errors for WaterRing consumption entry 0 ('Name' must be specified)";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void givenNoAbilityFoundForName_WhenLoading_ThrowsException() {
        givenConfigIs(ONE_ENTRY);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, this::whenLoading);
        String expectedMessage = "Encountered config errors for WaterRing consumption entry 0 (Could not find a class for ability by name AbilityName)";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void givenNoAbilityFoundForClass_WhenLoading_ThrowsException() {
        givenConfigIs(ONE_ENTRY_WITH_CLASS);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, this::whenLoading);
        String expectedMessage = "Encountered config errors for WaterRing consumption entry 0 (Could not find an ability by classname AbilityClass)";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void givenAbilityByNameHasNoSourceBlockGetter_WhenLoading_ThrowsException() {
        givenConfigIs(ONE_ENTRY);
        Mockito.when(abilityFinder.getAbilityByName("AbilityName")).thenReturn(new AbilityWithoutSourceGetter());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, this::whenLoading);
        String expectedMessage = "Encountered config errors for WaterRing consumption entry 0 (Ability by name AbilityName does not have a valid getSourceBlock method)";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void givenAbilityByClassHasNoSourceBlockGetter_WhenLoading_ThrowsException() {
        givenConfigIs(ONE_ENTRY_WITH_CLASS);
        Mockito.when(abilityFinder.getAbilityByClassName("AbilityClass")).thenReturn(new AbilityWithoutSourceGetter());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, this::whenLoading);
        String expectedMessage = "Encountered config errors for WaterRing consumption entry 0 (Ability by classname AbilityClass does not have a valid getSourceBlock method)";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void givenAbilityByNameHasInvalidSourceBlockGetter_WhenLoading_ThrowsException() {
        givenConfigIs(ONE_ENTRY);
        Mockito.when(abilityFinder.getAbilityByName("AbilityName")).thenReturn(new AbilityWithInvalidSourceGetter());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, this::whenLoading);
        String expectedMessage = "Encountered config errors for WaterRing consumption entry 0 (getSourceBlock method for name AbilityName does not return org.bukkit.block.Block)";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void givenAbilityByClassHasInvalidSourceBlockGetter_WhenLoading_ThrowsException() {
        givenConfigIs(ONE_ENTRY_WITH_CLASS);
        Mockito.when(abilityFinder.getAbilityByClassName("AbilityClass")).thenReturn(new AbilityWithInvalidSourceGetter());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, this::whenLoading);
        String expectedMessage = "Encountered config errors for WaterRing consumption entry 0 (getSourceBlock method for classname AbilityClass does not return org.bukkit.block.Block)";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    @Disabled("This test can't run because of the static initializer in ElementalAbility")
    public void givenConfigHasValidAbilityByName_WhenLoading_ShouldRegisterAndReturnConfigurationObject() {
        givenConfigIs(ONE_ENTRY);
        AbilityWithValidSourceGetter ability = Mockito.mock(AbilityWithValidSourceGetter.class);
        Mockito.when(abilityFinder.getAbilityByName("AbilityName")).thenReturn(ability);

        whenLoading();
        ConsumptionConfiguration consumptionConfiguration =
                consumptionConfigurationManager.getConfiguration(AbilityWithValidSourceGetter.class);
        consumptionConfiguration.getSourceBlock(ability);

        assertEquals(2, consumptionConfiguration.getUses());
        assertTrue(consumptionConfiguration.isRefundable());
        Mockito.verify(ability, Mockito.times(1)).getSourceBlock();
    }

    @Test
    @Disabled("This test can't run because of the static initializer in ElementalAbility")
    public void givenConfigHasValidAbilityByClassName_ShouldRegisterAndReturnConfigurationObject() {
        givenConfigIs(ONE_ENTRY_WITH_CLASS);
        AbilityWithValidSourceGetter ability = new AbilityWithValidSourceGetter();
        Mockito.when(abilityFinder.getAbilityByClassName("AbilityClass")).thenReturn(ability);

        whenLoading();
        ability.getSourceBlock();

        ConsumptionConfiguration consumptionConfiguration =
                consumptionConfigurationManager.getConfiguration(AbilityWithValidSourceGetter.class);
        assertEquals(2, consumptionConfiguration.getUses());
        assertTrue(consumptionConfiguration.isRefundable());
    }

    private void givenConfigIs(File configFile) {
        try {
            fileConfiguration.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void whenLoading() {
        this.consumptionConfigurationManager.load();
    }

    private void givenConfigContains(Object content) {
        fileConfiguration.set("ExtraAbilities.FinnBueno.WaterRing", content);
    }
}
