package com.flansmod.warforge.client;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.IngredientDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.StackComparable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreIngredient;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GUIUpgradePanel {
    //Dude why the fuck the documentation is so ass
    //Second attempt at using clientside UI, with static methods now


    public static ModularScreen createGui(UUID factionID, String factionName, int level, int color, boolean outrankingOfficer) {
        ListWidget list = new ListWidget<>()
                .scrollDirection(GuiAxis.Y)
                .keepScrollBarInArea(true)
                .sizeRel(0.95F, 0.90F)
                .background(GuiTextures.SLOT_ITEM)
                .paddingBottom(2)
                .align(Alignment.TopCenter);

// Sorted by quantity
        Map<StackComparable, Integer> requirements = WarForgeMod.UPGRADE_HANDLER.getRequirementsFor(level + 1)
                .entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        EntityPlayer player = Minecraft.getMinecraft().player;
        AtomicBoolean requirementPassed  = new AtomicBoolean(true);
        List<ItemStack> inventory = player.inventory.mainInventory;

        List<StackComparable.StackComparableResult> results = requirements.entrySet().stream()
                .map(entry -> {
                    StackComparable sc = entry.getKey();
                    int required = entry.getValue();

                    int count = inventory.stream()
                            .filter(stack -> !stack.isEmpty() && sc.equals(stack))
                            .mapToInt(ItemStack::getCount)
                            .sum();

                    if(count <= required)
                        requirementPassed.set(false);

                    return new StackComparable.StackComparableResult(sc, count, required);
                })
                .collect(Collectors.toList());

        AtomicInteger index = new AtomicInteger(0);
        results.forEach((comparableResult) -> {
            Ingredient ingredient;
            String displayName;

            if (comparableResult.compared.getOredict() != null) {
                ingredient = new OreIngredient(comparableResult.compared.getOredict());
                displayName = humanizeOreDictName(comparableResult.compared.getOredict());
            } else if (comparableResult.compared.getRegistryName() != null) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(comparableResult.compared.getRegistryName()));
                if (item == null || item == Items.AIR) {
                    WarForgeMod.LOGGER.error("Could not find item: " + comparableResult.compared.getRegistryName() + ". Skipping...");
                    return;
                }
                int meta = comparableResult.compared.getMeta() == -1 ? 0 : comparableResult.compared.getMeta();
                ItemStack stack = new ItemStack(item, 1, meta);
                ingredient = Ingredient.fromStacks(stack);
                displayName = stack.getDisplayName();
            } else {
                WarForgeMod.LOGGER.error("Malformed StackComparable: \n" + comparableResult + "\nSkipping...");
                return;
            }


            list.addChild(createRequirementRow(ingredient, displayName, comparableResult.required, comparableResult.has), index.getAndIncrement());
        });


        ModularPanel panel = ModularPanel.defaultPanel("citadel_upgrade_panel")
                .width(280);

        // Title label
        Widget prefix = IKey.str("Upgrade citadel for: ")
                .asWidget()
                .align(Alignment.TopCenter)
                .top(8)
                .scale(1.2f);

        Widget factionNamePlate = IKey.str(factionName)
                .asWidget()
                .relative(prefix)
                .color(color)
                .top(8)
                .scale(1.2f);


        Widget title = new Row()
                .align(Alignment.TopCenter)
                .paddingBottom(5)
                .marginBottom(5)
                .child(prefix)
                .child(factionNamePlate)
                .expanded()
                .height(30)

                ;

        // Close button
        ButtonWidget<?> closeButton = new ButtonWidget<>()
                .size(100, 16)
                .overlay(IKey.str("Close"))
                .onMousePressed(button -> {
                    panel.closeIfOpen(true);
                    return true;
                })
                .align(Alignment.CenterLeft);

        // Upgrade button
        ButtonWidget<?> upgradeButton = new ButtonWidget<>()
                .size(100, 16)
                .overlay(IKey.str("Upgrade"))
                .onMousePressed(button -> {
                    player.sendMessage(new TextComponentString("Hello " + player.getName()));
                    return true;
                })
                .setEnabledIf(x-> requirementPassed.get() && outrankingOfficer)
                .align(Alignment.CenterRight);

        // Button row
        Widget buttonRow = new Row()
                .child(closeButton)
                .child(upgradeButton)
                .align(Alignment.BottomCenter)
                .marginLeft(5)
                .marginRight(5)
                .height(32)
                ;

        // Main content column
        Widget content = new Column()
                .child(title)
                .child(list)
                .child(buttonRow)
                .sizeRel(0.98f, 0.95f)
                .align(Alignment.BottomCenter)
                ;

        // Assemble full panel
        panel
                .child(content);

        return new ModularScreen(panel);

    }


    public static String humanizeOreDictName(String oredict) {
        String name = oredict.startsWith("any") ? oredict.substring(3) : oredict;

        List<String> words = new ArrayList<>();
        Matcher m = Pattern.compile("([A-Z]?[a-z]+|[A-Z]+(?![a-z]))").matcher(name);
        while (m.find()) {
            words.add(m.group());
        }

        if (words.isEmpty()) {
            return "Any " + oredict;
        }

        Set<String> types = new HashSet<>(Arrays.asList(
                "ingot", "dust", "plate", "nugget", "ore", "gem", "block", "gear",
                "rod", "wire", "bolt", "screw", "foil", "tiny", "small", "cell",
                "dye", "plastic", "circuit"
        ));

        List<String> capitalized = new ArrayList<>();
        for (String word : words) {
            if (word.length() == 1) {
                capitalized.add(word.toUpperCase());
            } else {
                capitalized.add(Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase());
            }
        }

        String first = words.get(0).toLowerCase();
        if (types.contains(first)) {
            String typeWord = capitalized.remove(0);
            capitalized.add(typeWord);
        }

        return "Any " + String.join(" ", capitalized);

    }

    private static Row createRequirementRow(Ingredient ingredient, String displayName, int count, int has) {
        UITexture CHECK = UITexture.builder()
                .location(ModularUI.ID, "gui/widgets/toggle_config")
                .imageSize(14, 28)
                .uv(2, 16, 10, 10)
                .build();
        return (Row) new Row()
                .child(new IDrawable.DrawableWidget(new IngredientDrawable(ingredient))
                        .size(20, 20)
                        .align(Alignment.CenterLeft)
                )
                .child(IKey.str(displayName).asWidget()
                        .align(Alignment.CenterLeft)
                        .left(20 + 5 + 2)
                        .width(120)
                )

                .child(IKey.str("x " + count).asWidget()
                        .align(Alignment.CenterLeft)
                        .left(20 + 5 + 2 + 120)
                        .scale(1.5f)
                )
                .child(new IDrawable.DrawableWidget(CHECK)
                        .align(Alignment.CenterRight)
                        .setEnabledIf(x -> has >= count)
                        .size(20, 20)
                        .right(5)
                )
                .paddingLeft(5)
                .paddingRight(5)
                .margin(3, 2)
                .height(30)
                .background(GuiTextures.BUTTON_CLEAN)
                ;
    }


}
