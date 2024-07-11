/*
 * HISTORY
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:398:11/04/2018: Jacobian matrices of Cartesian <-> Spherical transformations
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.pvcoordinates;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class JacobianTransformationMatrixTest {
    /**
     * Test values of jacobians matrix for the transformation of cartesian
     * coordinates to spheric coordinates. Reference values are manually
     * validate and with MSLIB.
     * 
     * @throws PatriusException
     */
    @Test
    public void testJacobianCartesianToSpheric() throws PatriusException {
        // Position and velocity of the point to transform
        final PVCoordinates pv = new PVCoordinates(new Vector3D(30210, 6320, 14521), new Vector3D(12000, 4000, 3000));
        final double[] pvTab = new double[6];
        pvTab[0] = pv.getPosition().getX();
        pvTab[1] = pv.getPosition().getY();
        pvTab[2] = pv.getPosition().getZ();
        pvTab[3] = pv.getVelocity().getX();
        pvTab[4] = pv.getVelocity().getY();
        pvTab[5] = pv.getVelocity().getZ();

        // Jacobian to cartesian coordinates to spheric
        final Array2DRowRealMatrix mat = new Array2DRowRealMatrix(
            JacobianTransformationMatrix.getJacobianCartesianToSpheric(pv));
        final double[][] data = mat.getData(false);

        Assert.assertEquals(-1.2216557141526100E-05, data[0][0], 1e-16);
        Assert.assertEquals(-2.55573125238150E-06, data[0][1], 1e-16);
        Assert.assertEquals(2.65280912306697E-05, data[0][2], 1e-16);
        Assert.assertEquals(0, data[0][3], 1e-16);
        Assert.assertEquals(0, data[0][4], 1e-16);
        Assert.assertEquals(0, data[0][5], 1e-16);
        Assert.assertEquals(-6.6345680943410400E-06, data[1][0], 1e-16);
        Assert.assertEquals(3.17136554003232E-05, data[1][1], 1e-16);
        Assert.assertEquals(0, data[1][2], 1e-16);
        Assert.assertEquals(0, data[1][3], 1e-16);
        Assert.assertEquals(0, data[1][4], 1e-16);
        Assert.assertEquals(0, data[1][5], 1e-16);
        Assert.assertEquals(8.8568159705696500E-01, data[2][0], 1e-15);
        Assert.assertEquals(1.85286583694142E-01, data[2][1], 1e-15);
        Assert.assertEquals(4.25719380035226E-01, data[2][2], 1e-15);
        Assert.assertEquals(0, data[2][3], 1e-15);
        Assert.assertEquals(0, data[2][4], 1e-15);
        Assert.assertEquals(0, data[2][5], 1e-15);
        Assert.assertEquals(6.6557249087180800E-06, data[3][0], 1e-15);
        Assert.assertEquals(7.90027404355240E-07, data[3][1], 1e-15);
        Assert.assertEquals(-8.87164008484733E-06, data[3][2], 1e-15);
        Assert.assertEquals(-1.22165571415261E-05, data[3][3], 1e-15);
        Assert.assertEquals(-2.55573125238150E-06, data[3][4], 1e-15);
        Assert.assertEquals(2.65280912306698E-05, data[3][5], 1e-15);
        Assert.assertEquals(1.2027999703658500E-06, data[4][0], 1e-15);
        Assert.assertEquals(-1.32241125908153E-05, data[4][1], 1e-15);
        Assert.assertEquals(0, data[4][2], 1e-15);
        Assert.assertEquals(-6.63456809434104E-06, data[4][3], 1e-15);
        Assert.assertEquals(3.17136554003232E-05, data[4][4], 1e-15);
        Assert.assertEquals(0, data[4][5], 1e-15);
        Assert.assertEquals(2.3431780343517800E-02, data[5][0], 1e-15);
        Assert.assertEquals(4.85725334547896E-02, data[5][1], 1e-15);
        Assert.assertEquals(-6.98886092977028E-02, data[5][2], 1e-15);
        Assert.assertEquals(8.85681597056965E-01, data[5][3], 1e-15);
        Assert.assertEquals(1.85286583694142E-01, data[5][4], 1e-15);
        Assert.assertEquals(4.25719380035226E-01, data[5][5], 1e-15);
    }

    /**
     * Test the jacobian matrix for the transformation of spheric coordinates to
     * cartesian coordinates. Reference values are manually validate and with
     * MSLIB.
     * 
     * @throws PatriusException
     */
    @Test
    public void testJacobianSphericToCartesian() throws PatriusException {
        // Jacobian to spheric coordinates to cartesian coordinates
        final Vector3D pos = new Vector3D(9.0715e-2, 5.362077, 6.3779517E+06);
        final Vector3D vel = new Vector3D(1.8332e-5, 2.2060e-4, -181.488);

        final Array2DRowRealMatrix mat2 = new Array2DRowRealMatrix(
            JacobianTransformationMatrix.getJacobianSphericToCartesian(new PVCoordinates(pos, vel)));
        final double[][] dataInv = mat2.getData(false);

        Assert.assertEquals(-349522.705438807, dataInv[0][0], 1e-7);
        Assert.assertEquals(5.05770590615075000000000E+06, dataInv[0][1], 1e-7);
        Assert.assertEquals(6.02450638874989000000E-01, dataInv[0][2], 1e-15);
        Assert.assertEquals(0, dataInv[0][3], 1e-15);
        Assert.assertEquals(0, dataInv[0][4], 1e-15);
        Assert.assertEquals(0, dataInv[0][5], 1e-15);
        Assert.assertEquals(4.600724954245660E+05, dataInv[1][0], 1e-7);
        Assert.assertEquals(3.8424010763788200E+06, dataInv[1][1], 1e-7);
        Assert.assertEquals(-0.792998464718815, dataInv[1][2], 1e-15);
        Assert.assertEquals(0, dataInv[1][3], 1e-15);
        Assert.assertEquals(0, dataInv[1][4], 1e-15);
        Assert.assertEquals(0, dataInv[1][5], 1e-15);
        Assert.assertEquals(6351726.93563486, dataInv[2][0], 1e-7);
        Assert.assertEquals(0, dataInv[2][1], 1e-15);
        Assert.assertEquals(0.0905906323673605, dataInv[2][2], 1e-15);
        Assert.assertEquals(0, dataInv[2][3], 1e-15);
        Assert.assertEquals(0, dataInv[2][4], 1e-15);
        Assert.assertEquals(0, dataInv[2][5], 1e-15);
        Assert.assertEquals(-161.98503496993, dataInv[3][0], 1e-11);
        Assert.assertEquals(-999.987431800181, dataInv[3][1], 1e-11);
        Assert.assertEquals(0.000173930836237087, dataInv[3][2], 1e-15);
        Assert.assertEquals(-349522.705438807, dataInv[3][3], 1e-7);
        Assert.assertEquals(5057705.90615075, dataInv[3][4], 1e-7);
        Assert.assertEquals(0.602450638874989, dataInv[3][5], 1e-15);
        Assert.assertEquals(2.5215488630074, dataInv[4][0], 1e-11);
        Assert.assertEquals(999.984911112608, dataInv[4][1], 1e-11);
        Assert.assertEquals(0.000134222986736524, dataInv[4][2], 1e-15);
        Assert.assertEquals(460072.495424566, dataInv[4][3], 1e-7);
        Assert.assertEquals(3842401.07637882, dataInv[4][4], 1e-7);
        Assert.assertEquals(-0.7929984647188150000, dataInv[4][5], 1e-15);
        Assert.assertEquals(-191.3336724619690000000, dataInv[5][0], 1e-11);
        Assert.assertEquals(0, dataInv[5][1], 1e-15);
        Assert.assertEquals(0.0000182566227624549, dataInv[5][2], 1e-15);
        Assert.assertEquals(6351726.93563486, dataInv[5][3], 1e-7);
        Assert.assertEquals(0, dataInv[5][4], 1e-15);
        Assert.assertEquals(0.0905906323673605, dataInv[5][5], 1e-15);

    }
}
