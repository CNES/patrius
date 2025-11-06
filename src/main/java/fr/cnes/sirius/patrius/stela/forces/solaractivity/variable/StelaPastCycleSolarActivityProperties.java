package fr.cnes.sirius.patrius.stela.forces.solaractivity.variable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to store the past cycles solar activity files
 * 
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public final class StelaPastCycleSolarActivityProperties implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -6659516040955343118L;
    /**
     * Past cycles file paths
     */
    private static List<String> pastCycleFilePaths = new ArrayList<>();

    /**
     * private constructor
     */
    private StelaPastCycleSolarActivityProperties() {
    }

    /**
     * Set the past cycles file paths
     * 
     * @param pastCycleFilePaths the past cycles file paths
     */
    public static void setPastCycleFilePaths(final List<String> pastCycleFilePaths) {
        StelaPastCycleSolarActivityProperties.pastCycleFilePaths = pastCycleFilePaths;
    }

    /**
     * Get the past cycles file paths <br>
     * Warning : InputStreams can only be read once
     * 
     * @return the past cycles file paths
     * @throws IOException if an error occur while the creation of InputStreams
     */
    public static List<InputStream> getPastCycleFilePath() throws IOException {
        final List<InputStream> list = new ArrayList<>(pastCycleFilePaths.size());
        for (final String path : pastCycleFilePaths) {
            list.add(Files.newInputStream(Paths.get(path)));
        }
        return list;
    }
}
