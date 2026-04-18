package gg.alexandre.replay.ui.common;

public class CommonUI {

    public static final String DEFAULT_DROPDOWN_STYLE = """
            @Tick = "Sounds/TickActivate.ogg";
            @ButtonsCancelActivate = "Sounds/ButtonsCancelActivate.ogg";

            @SoundsDropdownBox = DropdownBoxSounds(
              Activate: (
                SoundPath: @Tick,
                Volume: 6
              ),
              MouseHover: (
                SoundPath: @ButtonsLightHover,
                Volume: 6
              ),
              Close: (
                SoundPath: @ButtonsCancelActivate,
                Volume: 6
              )
            );
            
            @ButtonsLightActivate = "Sounds/ButtonsLightActivate.ogg";
            @ButtonsLightHover = "Sounds/ButtonsLightHover.ogg";
            
            @SoundsButtonsLight = (
              Activate: (
                SoundPath: @ButtonsLightActivate,
                MinPitch: -0.4,
                MaxPitch: 0.4,
                Volume: 4
              ),
              MouseHover: (
                SoundPath: @ButtonsLightHover,
                Volume: 6
              )
            );
            
            @DefaultScrollbarStyle = ScrollbarStyle(
              Spacing: 6,
              Size: 6,
              Background: (TexturePath: "Common/Scrollbar.png", Border: 3),
              Handle: (TexturePath: "Common/ScrollbarHandle.png", Border: 3),
              HoveredHandle: (TexturePath: "Common/ScrollbarHandleHovered.png", Border: 3),
              DraggedHandle: (TexturePath: "Common/ScrollbarHandleDragged.png", Border: 3)
            );
            
            @DefaultDropdownBoxLabelStyle = LabelStyle(TextColor: #96a9be, RenderUppercase: true, VerticalAlignment: Center, FontSize: 13);
            @DefaultDropdownBoxEntryLabelStyle = LabelStyle(...@DefaultDropdownBoxLabelStyle, TextColor: #b7cedd);
            
            @DefaultDropdownBoxStyle = DropdownBoxStyle(
              DefaultBackground: (TexturePath: "Common/Dropdown.png", Border: 16),
              HoveredBackground: (TexturePath: "Common/DropdownHovered.png", Border: 16),
              PressedBackground: (TexturePath: "Common/DropdownPressed.png", Border: 16),
              DefaultArrowTexturePath: "Common/DropdownCaret.png",
              HoveredArrowTexturePath: "Common/DropdownCaret.png",
              PressedArrowTexturePath: "Common/DropdownPressedCaret.png",
              ArrowWidth: 13,
              ArrowHeight: 18,
              LabelStyle: @DefaultDropdownBoxLabelStyle,
              EntryLabelStyle: @DefaultDropdownBoxEntryLabelStyle,
              NoItemsLabelStyle: (...@DefaultDropdownBoxEntryLabelStyle, TextColor: #b7cedd(0.5)),
              SelectedEntryLabelStyle: (...@DefaultDropdownBoxEntryLabelStyle, RenderBold: true),
              HorizontalPadding: 8,
              PanelScrollbarStyle: @DefaultScrollbarStyle,
              PanelBackground: (TexturePath: "Common/DropdownBox.png", Border: 16),
              PanelPadding: 6,
              PanelAlign: Right,
              PanelOffset: 7,
              EntryHeight: 31,
              EntriesInViewport: 10,
              HorizontalEntryPadding: 7,
              HoveredEntryBackground: (Color: #0a0f17),
              PressedEntryBackground: (Color: #0f1621),
              Sounds: @SoundsDropdownBox,
              EntrySounds: @SoundsButtonsLight,
              FocusOutlineSize: 1,
              FocusOutlineColor: #ffffff(0.4)
            );
            """;

}
