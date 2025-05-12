package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;

import java.util.UUID;

public class GuiUpgradePanel extends CustomModularScreen {

    public UUID factionID = Faction.nullUuid;
    public String factionName = "placeholder";
    public int level;
    public int color = 0xffff;
    public boolean canClick = false;

    public GuiUpgradePanel(UUID factionID, String factionName, int level, int color, boolean canClick) {
        this.factionID = factionID;
        this.factionName = factionName;
        this.level = level;
        this.color = color;
        this.canClick = canClick;
    }


    @Override
    public ModularPanel buildUI(ModularGuiContext context) {


        ModularPanel panel = ModularPanel.defaultPanel("citadel_upgrade_panel").width(280).heightRel(0.8f);
        panel
                .child(IKey.str("Upgrade citadel for: " + factionName).asWidget()
                        .align(Alignment.TopCenter)
                        .top(8)
                        .scale(1.2f)
                )
                .child(new Column()

                        .child(new ListWidget<>()
                                .scrollDirection(GuiAxis.Y)
                                .keepScrollBarInArea(true)
                                .sizeRel(0.95F, 0.90F)
                                .background(GuiTextures.SLOT_ITEM)
                                .margin(10, 0, 5, 0)
                                .paddingBottom(2)
                                .align(Alignment.TopLeft)
                        )
                        .child(new Row()
                                .child(new ButtonWidget<>()
                                        .size(100, 16)
                                        .overlay(IKey.str("Close"))
                                        .onMousePressed(button -> {
                                            this.close();
                                            return true;
                                        })
                                        .align(Alignment.CenterLeft)
                                )
                                .child(new ButtonWidget<>()
                                        .size(100, 16)
                                        .overlay(IKey.str("Upgrade"))
                                        .onMousePressed(button -> {
                                            EntityPlayer player = Minecraft.getMinecraft().player;
                                            player.sendMessage(new TextComponentString("Hello " + player.getName()));
                                            return true;
                                        })
                                        .align(Alignment.CenterRight)
                                )
                                .align(Alignment.BottomCenter)
                                .height(32)
                                .marginLeft(10)
                        )
                        .sizeRel(0.9f, 0.95f)
                        .align(Alignment.BottomLeft)

                );
        return panel;
    }


}
