package pruebas;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GeneracionFicheros {


    public static void main(String[] args) {
        String outputDirPath = "roms-de-prueba-mal";  // Ruta donde se crean los archivos
        File outputDir = new File(outputDirPath);

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

            /*List<String> romFilenames = List.of(
                    "Super Mario World (USA).sfc",
                    "The Legend of Zelda - A Link to the Past (Europe).sfc",
                    "Pokémon Red Version (USA) (Rev 1).gb",
                    "Metroid Fusion (Japan).gba",
                    "Sonic the Hedgehog (Europe).gen",
                    "Tetris (World).gb",
                    "GoldenEye 007 (USA).n64",
                    "Final Fantasy VI (Spain).smc",
                    "Chrono Trigger (USA) (Beta).sfc",
                    "Castlevania - Symphony of the Night (USA).bin",
                    "Donkey Kong Country (USA).sfc",
                    "Super Metroid (Europe).sfc",
                    "The Legend of Zelda - Ocarina of Time (USA).n64",
                    "Mega Man X (Japan).sfc",
                    "EarthBound (USA).smc",
                    "Super Mario Bros. 3 (USA).nes",
                    "Contra (Japan).nes",
                    "Kirby’s Adventure (Europe).nes",
                    "Street Fighter II Turbo (USA).sfc",
                    "Pokémon Sapphire Version (Europe).gba",
                    "Advance Wars (USA).gba",
                    "Fire Emblem (Japan).gba",
                    "The Legend of Zelda - The Minish Cap (USA).gba",
                    "Wario Land 4 (Europe).gba",
                    "Donkey Kong Land (USA).gb",
                    "Kirby’s Dream Land (Japan).gb",
                    "Metroid II - Return of Samus (Europe).gb",
                    "Pokémon Yellow Version (USA).gb",
                    "Dr. Mario (USA).nes",
                    "Super Mario All-Stars (USA).sfc",
                    "Pac-Man (Europe).nes",
                    "Castlevania III - Dracula's Curse (USA).nes",
                    "DuckTales (Japan).nes",
                    "Bubble Bobble (Europe).nes",
                    "Mega Man 2 (USA).nes",
                    "Zelda II - The Adventure of Link (USA).nes",
                    "Super Mario 64 (USA).n64",
                    "Banjo-Kazooie (Europe).n64",
                    "Diddy Kong Racing (USA).n64",
                    "Paper Mario (USA).n64",
                    "Star Fox 64 (USA).n64",
                    "F-Zero X (Japan).n64",
                    "Mario Kart 64 (Europe).n64",
                    "1080° Snowboarding (USA).n64",
                    "Yoshi's Story (USA).n64",
                    "Bomberman 64 (Europe).n64",
                    "Harvest Moon 64 (USA).n64",
                    "Perfect Dark (USA).n64",
                    "Resident Evil 2 (USA).n64",
                    "Tony Hawk's Pro Skater (USA).n64",
                    "Pokémon Stadium (Europe).n64",
                    "Pokémon Snap (USA).n64",
                    "Excitebike 64 (Japan).n64",
                    "Pilotwings 64 (USA).n64",
                    "Wave Race 64 (Europe).n64",
                    "Killer Instinct Gold (USA).n64",
                    "Rayman 2 - The Great Escape (Europe).n64",
                    "Star Wars - Rogue Squadron (USA).n64",
                    "Battletoads (USA).nes",
                    "Double Dragon II - The Revenge (Europe).nes",
                    "Teenage Mutant Ninja Turtles (Japan).nes",
                    "Ninja Gaiden (USA).nes",
                    "Ice Climber (Europe).nes",
                    "Kid Icarus (USA).nes",
                    "Excitebike (USA).nes",
                    "Balloon Fight (Japan).nes",
                    "Duck Hunt (USA).nes",
                    "Tetris Attack (Europe).sfc",
                    "Donkey Kong Country 2 (USA).sfc",
                    "Secret of Mana (Japan).sfc",
                    "Terranigma (Europe).sfc",
                    "Illusion of Gaia (USA).sfc",
                    "Lufia II - Rise of the Sinistrals (USA).sfc",
                    "Breath of Fire II (USA).sfc",
                    "ActRaiser (Europe).sfc",
                    "Super Castlevania IV (USA).sfc",
                    "Axelay (USA).sfc",
                    "Gradius III (Japan).sfc",
                    "F-Zero (USA).sfc",
                    "Star Ocean (Japan) (Translated).sfc",
                    "Super Ghouls 'n Ghosts (USA).sfc",
                    "Pilotwings (USA).sfc",
                    "Super Punch-Out!! (USA).sfc",
                    "Zombies Ate My Neighbors (Europe).sfc",
                    "Uniracers (USA).sfc",
                    "Super Tennis (Europe).sfc",
                    "Kirby Super Star (USA).sfc",
                    "Kirby's Dream Course (USA).sfc",
                    "Yoshi's Island (USA).sfc",
                    "Doom (USA).sfc",
                    "Final Fantasy IV (Japan).sfc",
                    "Dragon Quest V (Japan) (Translated).sfc",
                    "Mother 3 (Japan) (Translated).gba",
                    "Castlevania - Aria of Sorrow (USA).gba",
                    "Mario & Luigi - Superstar Saga (Europe).gba",
                    "The Legend of Zelda - Oracle of Seasons (USA).gbc",
                    "The Legend of Zelda - Oracle of Ages (USA).gbc",
                    "Pokémon Crystal Version (Europe).gbc",
                    "Wario Land 3 (USA).gbc",
                    "Shantae (USA).gbc",
                    "Metal Gear Solid (Japan).gba",
                    "Advance Guardian Heroes (USA).gba",
                    "Gunstar Super Heroes (Europe).gba"
            );*/

        List<String> romFilenamesInvalid = List.of(
                "SuperMarioWorld.sfc",
                "Zelda_USA_final.gba",
                "Pokemon-Red.gb",
                "MarioBros3[USA].nes",
                "Donkey.Kong.Country.sfc",
                "mariokart64(USA).n64",      // falta espacio tras el título
                "007GoldenEye-USA.n64",
                "F-ZeroX - Japan.n64",
                "smb3usa.nes",
                "randomfile.txt"             // ni siquiera es una ROM
        );


        for (String filename : romFilenamesInvalid) {
            File romFile = new File(outputDir, filename);
            try {
                if (romFile.createNewFile()) {
                    System.out.println("Archivo creado: " + romFile.getAbsolutePath());
                } else {
                    System.out.println("Ya existía: " + romFile.getAbsolutePath());
                }
            } catch (IOException e) {
                System.err.println("Error al crear: " + filename);
                e.printStackTrace();
            }
        }
    }


}



