package fr.cnes.sirius.patrius.stela.forces.drag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.data.DataLoader;

/**
 * Class which reads values of a drag coefficient in a file.
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaVariableDragCoefReader implements DataLoader, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -503900655330815907L;

    /** Default file path. */
    public static final String DEFAULT_FILE = "/stela_drag_coefficient";

    /** Read coefficients [alt, dragCoef] (altitudes in [m]). */
    private final TreeMap<Double, Double> coefficients;

    /** Accept more data. */
    private boolean acceptMoreData;

    /**
     * Basis constructor.
     */
    public StelaVariableDragCoefReader() {
        this.coefficients = new TreeMap<>();
        this.acceptMoreData = true;
    }

    /**
     * Load the default data from the default resource file.
     * 
     * @throws IOException
     *         if error in searching or reading the file
     */
    public void loadDefaultData() throws IOException {
        final InputStream defaultStream = StelaVariableDragCoefReader.class.getResourceAsStream(DEFAULT_FILE);
        this.loadData(defaultStream, "");
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException {

        if (this.acceptMoreData) {
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.contains("#") || line.isEmpty()) {
                    // Skip the comment or empty lines
                    continue;
                }

                final String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    // Read the line with the correct format
                    try {
                        final double alt = Double.parseDouble(parts[0]); // in [m]
                        final double dragCoef = Double.parseDouble(parts[1]);
                        this.coefficients.put(alt, dragCoef);
                    } catch (final NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid format number in line : " + line, e.getCause());
                    }
                }
            }
            bufferedReader.close();
            this.acceptMoreData = false;
        } else {
            throw new IOException("Do not accept more data");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return this.acceptMoreData;
    }

    /**
     * Getter for the loaded coefficients.
     *
     * @return the loaded coefficients
     */
    public Map<Double, Double> getCoefficients() {
        return this.coefficients;
    }
}
