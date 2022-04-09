package dev.mja00.swarmsmps2.objects;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;

public class CharacterInfo {

    private String name;
    private String backstory;

    public CharacterInfo(String name, String backstory) {
        this.name = name;
        this.backstory = backstory;
    }

    public String getName() {
        return name;
    }

    public String getBackstory() {
        return backstory;
    }

    public MutableComponent getBackstoryAsComponent() {
        // We gotta do some really janky stuff here to get the text to look cool af
        // Split the text by word
        MutableComponent text = new TextComponent("");
        String[] words = backstory.split(" ");
        for (String word : words) {
            // Check if the string starts with &k
            MutableComponent currentWord;
            if (word.startsWith("&k")) {
                currentWord = new TextComponent(word.substring(2)).withStyle(ChatFormatting.OBFUSCATED);
            } else {
                currentWord = new TextComponent(word);
            }
            text.append(currentWord);
            // Add a space after each word unless it's the last one
            if (!word.equals(words[words.length - 1])) {
                text.append(new TextComponent(" "));
            }
        }
        return text;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBackstory(String backstory) {
        this.backstory = backstory;
    }

    @Override
    public String toString() {
        return "CharacterInfo{" + "name=" + name + ", backstory=" + backstory + '}';
    }

}
