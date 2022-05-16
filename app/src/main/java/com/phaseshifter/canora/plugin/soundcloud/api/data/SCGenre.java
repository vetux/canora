package com.phaseshifter.canora.plugin.soundcloud.api.data;

public enum SCGenre {
    ALLMUSIC("soundcloud:genres:all-music"),

    ALLAUDIO("soundcloud:genres:all-audio"),

    ALTERNATIVEROCK("soundcloud:genres:alternativerock"),

    AMBIENT("soundcloud:genres:ambient"),

    CLASSICAL("soundcloud:genres:classical"),

    COUNTRY("soundcloud:genres:country"),

    DANCEEDM("soundcloud:genres:danceedm"),

    DANCEHALL("soundcloud:genres:dancehall"),

    DEEPHOUSE("soundcloud:genres:deephouse"),

    DISCO("soundcloud:genres:disco"),

    DRUMBASS("soundcloud:genres:drumbass"),

    DUBSTEP("soundcloud:genres:dubstep"),

    ELECTRONIC("soundcloud:genres:electronic"),

    FOLKSINGERWRITER("soundcloud:genres:folksingersongwriter"),

    HIPHOPRAP("soundcloud:genres:hiphoprap"),

    HOUSE("soundcloud:genres:house"),

    INDIE("soundcloud:genres:indie"),

    JAZZBLUES("soundcloud:genres:jazzblues"),

    LATIN("soundcloud:genres:latin"),

    METAL("soundcloud:genres:metal"),

    PIANO("soundcloud:genres:piano"),

    POP("soundcloud:genres:pop"),

    RBSOUL("soundcloud:genres:rbsoul"),

    REGGAE("soundcloud:genres:reggae"),

    REGGAETON("soundcloud:genres:reggaeton"),

    ROCK("soundcloud:genres:rock"),

    SOUNDTRACK("soundcloud:genres:soundtrack"),

    TECHNO("soundcloud:genres:techno"),

    TRANCE("soundcloud:genres:trance"),

    TRAP("soundcloud:genres:trap"),

    TRIPHOP("soundcloud:genres:triphop"),

    WORLD("soundcloud:genres:world"),

    AUDIOBOOKS("soundcloud:genres:audiobooks"),

    BUSINESS("soundcloud:genres:business"),

    COMEDY("soundcloud:genres:comedy"),

    ENTERTAINMENT("soundcloud:genres:entertainment"),

    LEARNING("soundcloud:genres:learning"),

    NEWSPOLITICS("soundcloud:genres:newspolitics"),

    RELIGIONSPIRITUALITY("soundcloud:genres:religionspirituality"),

    SCIENCE("soundcloud:genres:science"),

    SPORTS("soundcloud:genres:sports"),

    STORYTELLING("soundcloud:genres:storytelling"),

    TECHNOLOGY("soundcloud:genres:technology");

    public final String parameterValue;

    SCGenre(String parameterValue) {
        this.parameterValue = parameterValue;
    }

    public static SCGenre fromString(String genreString) {
        for (int i = 0; i < SCGenre.values().length; i++) {
            if (SCGenre.values()[i].parameterValue.equals(genreString)) {
                return SCGenre.values()[i];
            }
        }
        return null;
    }
}
