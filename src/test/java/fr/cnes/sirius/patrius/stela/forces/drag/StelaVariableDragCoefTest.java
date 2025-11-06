package fr.cnes.sirius.patrius.stela.forces.drag;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.stela.bodies.GeodPosition;
import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tests for the {@link StelaVariableDragCoef} and {@link StelaVariableDispersedDragCoef} classes
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaVariableDragCoefTest {

    /**
     * Expected coefficients.
     */
    private static final TreeMap<Double, Double> expectedCoefficients = new TreeMap<>();

    /**
     * Common drag reader
     */
    private static final StelaVariableDragCoefReader reader = new StelaVariableDragCoefReader();

    /**
     * Earth flattening stela constant
     */
    private final double flattening = 0.29825765000000E+03;

    /**
     * Default variable drag coefficient
     */
    private final StelaVariableDragCoef defaultDragCoefficient = new StelaVariableDragCoef(reader.getCoefficients(),
        Constants.CNES_STELA_AE, 1 / this.flattening);

    /**
     * Dispersed drag coefficient
     */
    private final double dispersedCoef = 2.;

    /**
     * Default dispersed variable drag coefficient
     */
    final StelaVariableDispersedDragCoef defaultDispersedDragCoefficient =
        new StelaVariableDispersedDragCoef(reader.getCoefficients(), Constants.CNES_STELA_AE, 1 / this.flattening,
            this.dispersedCoef);

    static {
        try {
            reader.loadDefaultData();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        expectedCoefficients.put(120., 2.07);
        expectedCoefficients.put(130., 2.08);
        expectedCoefficients.put(155., 2.09);
        expectedCoefficients.put(195., 2.10);
        expectedCoefficients.put(260., 2.11);
        expectedCoefficients.put(375., 2.12);
        expectedCoefficients.put(435., 2.13);
        expectedCoefficients.put(460., 2.14);
        expectedCoefficients.put(480., 2.15);
        expectedCoefficients.put(500., 2.16);
        expectedCoefficients.put(515., 2.17);
        expectedCoefficients.put(530., 2.18);
        expectedCoefficients.put(545., 2.19);
        expectedCoefficients.put(555., 2.20);
        expectedCoefficients.put(570., 2.21);
        expectedCoefficients.put(580., 2.22);
        expectedCoefficients.put(590., 2.23);
        expectedCoefficients.put(600., 2.24);
        expectedCoefficients.put(605., 2.25);
        expectedCoefficients.put(615., 2.26);
        expectedCoefficients.put(620., 2.27);
        expectedCoefficients.put(630., 2.28);
        expectedCoefficients.put(635., 2.29);
        expectedCoefficients.put(645., 2.30);
        expectedCoefficients.put(650., 2.31);
        expectedCoefficients.put(655., 2.32);
        expectedCoefficients.put(665., 2.33);
        expectedCoefficients.put(670., 2.34);
        expectedCoefficients.put(675., 2.35);
        expectedCoefficients.put(680., 2.36);
        expectedCoefficients.put(685., 2.37);
        expectedCoefficients.put(690., 2.38);
        expectedCoefficients.put(695., 2.39);
        expectedCoefficients.put(700., 2.40);
        expectedCoefficients.put(705., 2.41);
        expectedCoefficients.put(710., 2.42);
        expectedCoefficients.put(715., 2.43);
        expectedCoefficients.put(720., 2.44);
        expectedCoefficients.put(725., 2.45);
        expectedCoefficients.put(730., 2.46);
        expectedCoefficients.put(735., 2.47);
        expectedCoefficients.put(740., 2.48);
        expectedCoefficients.put(745., 2.49);
        expectedCoefficients.put(750., 2.50);
        expectedCoefficients.put(755., 2.51);
        expectedCoefficients.put(760., 2.52);
        expectedCoefficients.put(765., 2.53);
        expectedCoefficients.put(770., 2.54);
        expectedCoefficients.put(775., 2.55);
        expectedCoefficients.put(780., 2.56);
        expectedCoefficients.put(785., 2.57);
        expectedCoefficients.put(790., 2.58);
        expectedCoefficients.put(795., 2.59);
        expectedCoefficients.put(800., 2.60);
        expectedCoefficients.put(805., 2.61);
        expectedCoefficients.put(810., 2.62);
        expectedCoefficients.put(815., 2.63);
        expectedCoefficients.put(820., 2.64);
        expectedCoefficients.put(825., 2.65);
        expectedCoefficients.put(830., 2.66);
        expectedCoefficients.put(835., 2.67);
        expectedCoefficients.put(845., 2.68);
        expectedCoefficients.put(850., 2.69);
        expectedCoefficients.put(855., 2.70);
        expectedCoefficients.put(860., 2.71);
        expectedCoefficients.put(865., 2.72);
        expectedCoefficients.put(875., 2.73);
        expectedCoefficients.put(880., 2.74);
        expectedCoefficients.put(885., 2.75);
        expectedCoefficients.put(895., 2.76);
        expectedCoefficients.put(900., 2.77);
        expectedCoefficients.put(910., 2.78);
        expectedCoefficients.put(915., 2.79);
        expectedCoefficients.put(925., 2.80);
        expectedCoefficients.put(935., 2.81);
        expectedCoefficients.put(945., 2.82);
        expectedCoefficients.put(955., 2.83);
        expectedCoefficients.put(965., 2.84);
        expectedCoefficients.put(975., 2.85);
        expectedCoefficients.put(990., 2.86);
        expectedCoefficients.put(1005., 2.87);
        expectedCoefficients.put(1020., 2.88);
        expectedCoefficients.put(1035., 2.89);
        expectedCoefficients.put(1055., 2.90);
        expectedCoefficients.put(1080., 2.91);
        expectedCoefficients.put(1110., 2.92);
        expectedCoefficients.put(1145., 2.93);
        expectedCoefficients.put(1190., 2.94);
        expectedCoefficients.put(1245., 2.95);
    }

    /* **************************** Variable ***************************** */

    /**
     * Method tested : <br>
     * - {@link StelaVariableDragCoef#getDragCoef(StelaDragCoefInput)}
     */
    @Test
    public void testGetDragCoef() throws PatriusException {
        double actualValue;
        for (int i = 120; i < 1020; i += 20) {
            final Vector3D position = new Vector3D(0, 0, i * Constants.KM_TO_M + Constants.CNES_STELA_AE);
            final double geodeticAlt =
                this.defaultDragCoefficient.getGeodPosition().getGeodeticAltitude(position) / Constants.KM_TO_M;
            actualValue = this.defaultDragCoefficient.getDragCoef(new StelaDragCoefInput(position));

            assertEquals(expectedCoefficients.floorEntry(geodeticAlt).getValue(), actualValue,
                Precision.DOUBLE_COMPARISON_EPSILON);
        }
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDragCoef#getCdMap()}
     */
    @Test
    public void testGetCdMap() {
        Assert.assertArrayEquals(reader.getCoefficients().keySet().toArray(),
            this.defaultDragCoefficient.getCdMap().keySet().toArray());
        Assert.assertArrayEquals(reader.getCoefficients().values().toArray(),
            this.defaultDragCoefficient.getCdMap().values().toArray());
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDragCoef#getGeodPosition()}
     */
    @Test
    public void testGetGeodPosition() {
        final GeodPosition geod = new GeodPosition(Constants.CNES_STELA_AE, 1 / this.flattening);
        final StelaVariableDragCoef drag = new StelaVariableDragCoef(null, geod);
        Assert.assertEquals(drag.getGeodPosition(), geod);
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDragCoef#toString()}
     */
    @Test
    public void testToString() {
        Assert.assertEquals("Drag Coefficient Type : VARIABLE", this.defaultDragCoefficient.toString());
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDragCoef#copy()}
     */
    @Test
    public void testCopy() {
        final StelaVariableDragCoef copy = this.defaultDragCoefficient.copy();
        Assert.assertNotEquals(this.defaultDragCoefficient, copy);
        Assert.assertEquals(this.defaultDragCoefficient.getCdMap(), copy.getCdMap());
        Assert.assertEquals(this.defaultDragCoefficient.getGeodPosition(), copy.getGeodPosition());
    }

    /* ************************ Variable Dispersed ************************ */

    /**
     * Method tested : <br>
     * - {@link StelaVariableDispersedDragCoef#StelaVariableDispersedDragCoef(Map, double, double)}
     */
    @Test
    public void testDispersedConstructor() {
        final StelaVariableDispersedDragCoef dragCoef = new StelaVariableDispersedDragCoef(null, 0, 0);
        Assert.assertEquals(1, dragCoef.getCoef(), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Test the {@link StelaVariableDispersedDragCoef#getDragCoef(StelaDragCoefInput)} method
     */
    @Test
    public void testDispersedGetDragCoef() throws PatriusException {
        double value;

        for (int i = 120; i < 1020; i += 20) {
            final Vector3D position = new Vector3D(0, 0, i * Constants.KM_TO_M + Constants.CNES_STELA_AE);
            final double geodeticAlt =
                this.defaultDispersedDragCoefficient.getGeodPosition().getGeodeticAltitude(position)
                        / Constants.KM_TO_M;
            value = this.defaultDispersedDragCoefficient.getDragCoef(new StelaDragCoefInput(position));

            assertEquals(expectedCoefficients.floorEntry(geodeticAlt).getValue() * this.dispersedCoef, value,
                Precision.DOUBLE_COMPARISON_EPSILON);
        }
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDispersedDragCoef#getCoef()}
     */
    @Test
    public void testDispersedGetCoef() {
        Assert.assertEquals(this.dispersedCoef, this.defaultDispersedDragCoefficient.getCoef(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDispersedDragCoef#toString()}
     */
    @Test
    public void testDispersedToString() {
        final String CR = System.lineSeparator();
        Assert.assertEquals(
            "Drag Coefficient Type : VARIABLE_DISPERSED" + CR + "Dispersion coef : " + this.dispersedCoef + CR,
            this.defaultDispersedDragCoefficient.toString());
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDispersedDragCoef#getStatInformation()}
     */
    @Test
    public void testDispersedGetStatInformation() {
        Assert.assertEquals("2", this.defaultDispersedDragCoefficient.getStatInformation());
    }

    /**
     * Method tested : <br>
     * - {@link StelaVariableDispersedDragCoef#copy()}
     */
    @Test
    public void testDispersedCopy() {
        final StelaVariableDispersedDragCoef copy = this.defaultDispersedDragCoefficient.copy();
        Assert.assertNotEquals(this.defaultDispersedDragCoefficient, copy);
        Assert.assertEquals(this.defaultDispersedDragCoefficient.getCdMap(), copy.getCdMap());
        Assert.assertEquals(this.defaultDispersedDragCoefficient.getGeodPosition(), copy.getGeodPosition());
    }
}
