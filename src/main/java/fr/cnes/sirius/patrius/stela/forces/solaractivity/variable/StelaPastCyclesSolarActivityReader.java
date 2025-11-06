package fr.cnes.sirius.patrius.stela.forces.solaractivity.variable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class to read solar activity past cycles files
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaPastCyclesSolarActivityReader implements DataLoader, Serializable {

    /** Expected columns number of the PastCyclesACSOL file. */
    private static final int EXPECTED_COLUMNS_NUMBER = 9;

    /** Second dimension for AP */
    private static final int AP_DIM = 8;

    /** Solar flux map. */
    private final TreeMap<Double, List<Double>> solarFluxMap = new TreeMap<>();
    /** Solar AP map. */
    private final TreeMap<Double, List<Double>> solarAPMap = new TreeMap<>();

    /** File index */
    private double fileIndex = 0;

    /** Indicator for completed read. */
    private boolean readCompleted;

    /**
     * Constructor.
     */
    public StelaPastCyclesSolarActivityReader() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException,
        PatriusException {

        // buffer file data
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {

            if (line.contains("#")) {
                // Skip header
                continue;
            }

            final String[] tab = line.trim().split("\\s+");
            if (tab.length != EXPECTED_COLUMNS_NUMBER) {
                // incorrect number of elements
                String loaderName = this.getClass().getName();
                loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
                throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER, name, loaderName);
            }

            // Flux
            final double flux = Double.parseDouble(tab[0]);

            List<Double> dataListFlux = this.solarFluxMap.get(this.fileIndex);
            if (dataListFlux == null) {
                dataListFlux = new ArrayList<>();
            }
            dataListFlux.add(flux);
            this.solarFluxMap.put(this.fileIndex, dataListFlux);

            // AP
            List<Double> dataListAp = this.solarAPMap.get(this.fileIndex);
            if (dataListAp == null) {
                dataListAp = new ArrayList<>(AP_DIM);
            }

            for (int i = 0; i < AP_DIM; i++) {
                final double temp = Double.parseDouble(tab[i + 1]);
                dataListAp.add(temp);
            }

            this.solarAPMap.put(this.fileIndex, dataListAp);
        }
        this.fileIndex++;
        bufferedReader.close();
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return !this.readCompleted;
    }

    public NavigableMap<Double, List<Double>> getSolarFluxMap() {
        return this.solarFluxMap;
    }

    public NavigableMap<Double, List<Double>> getSolarAPMap() {
        return this.solarAPMap;
    }

    public void setReadCompleted(final boolean readCompleted) {
        this.readCompleted = readCompleted;
    }
}
