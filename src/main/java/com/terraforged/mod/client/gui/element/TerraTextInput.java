package com.terraforged.mod.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TerraTextInput extends TextFieldWidget implements Element, Consumer<String> {

    private final String name;
    private final CompoundNBT value;
    private final List<String> tooltip;

    private String stringValue = "";
    private boolean valid = true;
    private Predicate<String> validator = s -> true;
    private Consumer<TerraTextInput> callback = t -> {};

    public TerraTextInput(String name, CompoundNBT value) {
        super(Minecraft.getInstance().fontRenderer, 0, 0, 100, 20, new StringTextComponent(Element.getDisplayName(name, value) + ": "));
        this.name = name;
        this.value = value;
        this.tooltip = Element.getToolTip(name, value);
        this.stringValue = value.getString(name);
        setText(value.getString(name));
        setResponder(this);
        setEnabled(true);
    }

    public boolean isValid() {
        return valid;
    }

    public String getValue() {
        return stringValue;
    }

    public void setColorValidator(Predicate<String> validator) {
        this.validator = validator;

        // update validity immediately
        if (validator.test(stringValue)) {
            valid = true;
            setTextColor(14737632);
        } else {
            valid = false;
            setTextColor(0xffff3f30);
        }
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        return super.keyPressed(i, j, k);
    }

    @Override
    public boolean charTyped(char c, int code) {
        return super.charTyped(c, code);
    }

    @Override
    public List<String> getTooltip() {
        return tooltip;
    }

    @Override
    public void accept(String text) {
        value.put(name, StringNBT.valueOf(text));

        stringValue = text;
        if (validator.test(text)) {
            valid = true;
            setTextColor(14737632);
        } else {
            valid = false;
            setTextColor(0xffff3f30);
        }

        callback.accept(this);
    }
}
