/**
 * 
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:457:09/11/2015: Move extendedOneAxisEllipsoid from patrius to orekit addons
 * VERSION::DM:1420:24/11/2017:updateMainPartFrame() speed-up
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test assembly part frames mechanisms
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class TestPartsOrientations {

    /** Main part */
    final String mPart = "mainPart";

    /** Facet */
    final String facet = "facet";

    /** Date */
    final AbsoluteDate date = new AbsoluteDate();

    /** Sun */
    final CelestialBody sun = new CelestialBody(){

        private static final long serialVersionUID = -980113647486139909L;

        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
            return new PVCoordinates(new Vector3D(0, -1E11, -6E10), Vector3D.ZERO);
        }

        @Override
        public String getName() {
            return "sun";
        }

        @Override
        public Frame getInertiallyOrientedFrame() throws PatriusException {
            return null;
        }

        @Override
        public double getGM() {
            return 0;
        }

        @Override
        public Frame getBodyOrientedFrame() throws PatriusException {
            return null;
        }
    };

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Vehicle parts
         * 
         * @featureDescription Test correct account of parts orientations
         * 
         * @coveredRequirements DV-VEHICULE_50, DV-VEHICULE_70, DV-VEHICULE_80
         */
        ASSEMBLY_FRAME_TRANSFORMATIONS
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_FRAME_TRANSFORMATIONS}
     * 
     * @testedMethod {@link IPart#getFrame()}
     * @testedMethod {@link Frame#getTransformTo(Frame, AbsoluteDate)}
     * 
     * @description creation of an assembly containing a facet with radiative property. given <br>
     * 
     *              <pre>
     * x<sub>sun</sub> = 0
     * </pre>
     * 
     * <br>
     *              we compute the resulting PRS for varying orienations of the facet. Here the facet is oriented away
     *              from the sun, and the PRS is zero.
     * @inputAn assembly
     * 
     * @output the PRS
     * 
     * @testPassCriteria the PRS is the expected one
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void test() throws PatriusException {

        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        /*
         * SunX = 0
         */
        // System.out.println(sun.getPVCoordinates(date, gcrf).getPosition().getX());
        // System.out.println(sun.getPVCoordinates(date, gcrf).getPosition().getY());
        // System.out.println(sun.getPVCoordinates(date, gcrf).getPosition().getZ());

        /*
         * Oriented and displaced facet
         */

        final RadiativeProperty rp = new RadiativeProperty(0, .8, 0);
        final Facet f = new Facet(Vector3D.PLUS_K, 5);
        final RadiativeFacetProperty rfp = new RadiativeFacetProperty(f);
        final MassProperty mass = new MassProperty(2000);

        final double angle = 0; // FastMath.PI ;

        final Rotation rot = new Rotation(Vector3D.PLUS_I, angle);
        final Transform rott = new Transform(this.date, rot);

        final AssemblyBuilder builder = new AssemblyBuilder();

        builder.addMainPart(this.mPart);
        builder.addPart(this.facet, this.mPart, rott);

        builder.addProperty(rp, this.facet);
        builder.addProperty(rfp, this.facet);
        builder.addProperty(mass, this.facet);

        final Assembly sc = builder.returnAssembly();

        final DirectRadiativeModel model = new DirectRadiativeModel(sc);

        /*
         * Orbit
         */
        final double ae = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;
        final double alt = 2000000;
        final double r = MathLib.sqrt(alt * alt + ae * ae);
        final double mu = Constants.EGM96_EARTH_MU;
        final double flat = Constants.GRIM5C1_EARTH_FLATTENING;
        final Vector3D pos = new Vector3D(0, -r, -r);
        final Vector3D vel = Vector3D.ZERO;
        final PVCoordinates pv = new PVCoordinates(pos, vel);

        final CartesianOrbit orbit = new CartesianOrbit(pv, gcrf, this.date, mu);

        final Attitude attitude = new Attitude(this.date, gcrf, AngularCoordinates.IDENTITY);

        final SpacecraftState scs = new SpacecraftState(orbit, attitude, new MassModel(sc));
        sc.initMainPartFrame(scs);

        /*
         * PRS
         */
        final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(ae, flat, itrf, "earth");
        final SolarRadiationPressureEllipsoid prs = new SolarRadiationPressureEllipsoid(this.sun, earth, model);

        final Vector3D acc = prs.computeAcceleration(scs);

        Assert.assertEquals(0, acc.getX(), Precision.EPSILON);
        Assert.assertEquals(0, acc.getY(), Precision.EPSILON);
        Assert.assertEquals(0, acc.getZ(), Precision.EPSILON);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ASSEMBLY_FRAME_TRANSFORMATIONS}
     * 
     * @testedMethod {@link IPart#getFrame()}
     * @testedMethod {@link Frame#getTransformTo(Frame, AbsoluteDate)}
     * 
     * @description creation of an assembly containing a facet with radiative property. given <br>
     * 
     *              <pre>
     * x<sub>sun</sub> = 0
     * </pre>
     * 
     * <br>
     *              we compute the resulting PRS for varying orienations of the facet. Here the facet is oriented
     *              towards the sun, and the PRS is not zero.
     * @inputAn assembly
     * 
     * @output the PRS
     * 
     * @testPassCriteria the PRS is the expected one
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void testForce() throws PatriusException {

        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        /*
         * SunX = 0
         */
        // System.out.println(sun.getPVCoordinates(date, gcrf).getPosition().getX());
        // System.out.println(sun.getPVCoordinates(date, gcrf).getPosition().getY());
        // System.out.println(sun.getPVCoordinates(date, gcrf).getPosition().getZ());

        /*
         * Oriented and displaced facet
         */

        final RadiativeProperty rp = new RadiativeProperty(.9, .8, .7);
        final Facet f = new Facet(Vector3D.PLUS_K, 5);
        final RadiativeFacetProperty rfp = new RadiativeFacetProperty(f);
        final MassProperty mass = new MassProperty(2000);

        final double angle = FastMath.PI;

        final Rotation rot = new Rotation(Vector3D.PLUS_I, angle);
        final Transform rott = new Transform(this.date, rot);

        final AssemblyBuilder builder = new AssemblyBuilder();

        builder.addMainPart(this.mPart);
        builder.addPart(this.facet, this.mPart, rott);

        builder.addProperty(rp, this.facet);
        builder.addProperty(rfp, this.facet);
        builder.addProperty(mass, this.facet);

        final Assembly sc = builder.returnAssembly();

        final DirectRadiativeModel model = new DirectRadiativeModel(sc);

        /*
         * Orbit
         */
        final double ae = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;
        final double alt = 2000000;
        final double r = MathLib.sqrt(alt * alt + ae * ae);
        final double mu = Constants.EGM96_EARTH_MU;
        final double flat = Constants.GRIM5C1_EARTH_FLATTENING;
        final Vector3D pos = new Vector3D(0, -r, -r);
        final Vector3D vel = Vector3D.ZERO;
        final PVCoordinates pv = new PVCoordinates(pos, vel);

        final CartesianOrbit orbit = new CartesianOrbit(pv, gcrf, this.date, mu);

        final Attitude attitude = new Attitude(this.date, gcrf, AngularCoordinates.IDENTITY);

        final SpacecraftState scs = new SpacecraftState(orbit, attitude, new MassModel(sc));
        sc.initMainPartFrame(scs);

        /*
         * PRS
         */
        final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(ae, flat, itrf, "earth");
        final SolarRadiationPressureEllipsoid prs = new SolarRadiationPressureEllipsoid(this.sun, earth, model);

        final Vector3D acc = prs.computeAcceleration(scs);

        Assert.assertEquals(0, acc.getX(), Precision.EPSILON);
        Assert.assertEquals(1.3245120216684912E-8, acc.getY(), Precision.EPSILON);
        Assert.assertEquals(2.039856288450499E-8, acc.getZ(), Precision.EPSILON);

    }
}