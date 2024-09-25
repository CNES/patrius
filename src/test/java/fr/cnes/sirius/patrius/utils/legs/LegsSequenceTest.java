/**
 * Copyright 2011-2022 CNES
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
 * HISTORY
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3125:10/05/2022:[PATRIUS] La sequence renvoyee par Sequences.unmodifiableXXX est modifiable 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.9:DM:DM-3081:24/02/2022:[PATRIUS] Ajout du numero du segment et impression de la chaine de caracteres
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:DM:DM-2352:27/05/2020:Modification du message d'erreur de la methode getLeg() de AbstractLegsSequence
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils.legs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.attitudes.AttitudeLawLeg;
import fr.cnes.sirius.patrius.attitudes.AttitudeLeg;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.StrictAttitudeLegsSequence;
import fr.cnes.sirius.patrius.attitudes.SunPointing;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * @description <p>
 *              Tests for the legs package.
 *              </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: AeroAttitudeLawTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 4.2
 */
public class LegsSequenceTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle leg and legs sequence
         * 
         * @featureDescription object describing a legs sequence
         * 
         * @coveredRequirements DM-1948
         */
        LEGS_SEQUENCE
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(LegsSequenceTest.class.getSimpleName(), "Legs sequence");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LEGS_SEQUENCE}
     * 
     * @testedMethod {@link LegsSequence#getAttitude()}
     * 
     * @description test attitude computation methods of legs sequence
     * 
     * @input a leg sequence:
     *        - Body center pointing rotation on [AbsoluteDate.J2000_EPOCH; AbsoluteDate.J2000_EPOCH + 10s]
     *        - Sun pointing on [AbsoluteDate.J2000_EPOCH + 10s; AbsoluteDate.J2000_EPOCH + 20s]
     *        - Constant rotation on [AbsoluteDate.J2000_EPOCH + 20s; AbsoluteDate.J2000_EPOCH + 30s]
     * 
     * @output orientation at different dates
     * 
     * @testPassCriteria rotation is as expected (reference: math)
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testNumericalLegsSequence() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testNumericalLegsSequence", "Angle computation", "Math", 1E-14,
            ComparisonType.ABSOLUTE);

        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(7000000, 6000000, 5000000), new Vector3D(1000, 7000, 2000), Vector3D.ZERO);
        final Vector3D pos = pv.getPosition();
        final PVCoordinatesProvider pvProvider = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -4283626681085109147L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return pv;
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        // Build profile
        final StrictAttitudeLegsSequence<AttitudeLeg> provider = new StrictAttitudeLegsSequence<>();
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AttitudeLeg leg1 = new AttitudeLawLeg(new BodyCenterPointing(), date1, date2);
        final AttitudeLeg leg2 =
            new AttitudeLawLeg(new SunPointing(Vector3D.PLUS_K, Vector3D.PLUS_I, new MeeusSun()), date2, date3);
        final AttitudeLeg leg3 =
            new AttitudeLawLeg(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY), date3, date4);
        provider.add(leg1);
        provider.add(leg2);
        provider.add(leg3);

        // Computation and check angle on each segment
        // Each segment performs a rotation of [0, 0, 1] vector
        final Rotation rot1 =
            provider.getAttitude(pvProvider, date1.shiftedBy(0), FramesFactory.getGCRF()).getRotation();
        final Rotation rot2 =
            provider.getAttitude(pvProvider, date2.shiftedBy(1), FramesFactory.getGCRF()).getRotation();
        final Rotation rot3 =
            provider.getAttitude(pvProvider, date3.shiftedBy(1), FramesFactory.getGCRF()).getRotation();
        final Vector3D actual1 = rot1.applyTo(Vector3D.PLUS_K);
        final Vector3D actual2 = rot2.applyTo(Vector3D.PLUS_K);
        final Vector3D actual3 = rot3.applyTo(Vector3D.PLUS_K);
        final Vector3D expected1 = pos.normalize().negate();
        final Vector3D expected2 =
            new MeeusSun().getPVCoordinates(date2.shiftedBy(1), FramesFactory.getGCRF()).getPosition().subtract(pos)
                .normalize();
        final Vector3D expected3 = Vector3D.PLUS_K;
        Report.printToReport("Image of +K on 1st segment", expected1, actual1);
        Report.printToReport("Image of +K on 2nd segment", expected2, actual2);
        Report.printToReport("Image of +K on 3rd segment", expected3, actual3);
        Assert.assertEquals(0., expected1.subtract(actual1).getNorm(), 1E-6);
        Assert.assertEquals(0., expected2.subtract(actual2).getNorm(), 1E-6);
        Assert.assertEquals(0., expected3.subtract(actual3).getNorm(), 1E-6);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LEGS_SEQUENCE}
     * 
     * @testedMethod all legs sequence methods (set methods not included)
     * 
     * @description test all functional methods of legs sequence (set methods not included)
     * 
     * @input a leg sequence:
     *        - Body center pointing rotation on [AbsoluteDate.J2000_EPOCH; AbsoluteDate.J2000_EPOCH + 10s]
     *        - Sun pointing on [AbsoluteDate.J2000_EPOCH + 10s; AbsoluteDate.J2000_EPOCH + 20s]
     *        - Constant rotation on [AbsoluteDate.J2000_EPOCH + 20s; AbsoluteDate.J2000_EPOCH + 30s]
     * 
     * @output output of all methods
     * 
     * @testPassCriteria result is as expected (functional test)
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testFunctionalLegsSequence() throws PatriusException {

        // Initialization
        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(7000000, 6000000, 5000000), new Vector3D(1000, 7000, 2000));
        final PVCoordinatesProvider pvProvider = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 8968817001106980298L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return pv;
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        // Build profile
        final StrictAttitudeLegsSequence<AttitudeLeg> provider = new StrictAttitudeLegsSequence<>();
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AbsoluteDate date5 = AbsoluteDate.J2000_EPOCH.shiftedBy(100.);
        final AbsoluteDate date6 = AbsoluteDate.J2000_EPOCH.shiftedBy(200.);
        final AttitudeLeg leg1 = new AttitudeLawLeg(new BodyCenterPointing(), date1, date2);
        final AttitudeLeg leg2 =
            new AttitudeLawLeg(new SunPointing(Vector3D.PLUS_K, Vector3D.PLUS_I, new MeeusSun()), date2, date3);
        final AttitudeLeg leg3 =
            new AttitudeLawLeg(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY), date3, date4);
        provider.add(leg1);
        provider.add(leg2);
        provider.add(leg3);

        // Check exception (date outside interval)
        // Compact sequence cases
        try {
            provider.getAttitude(pvProvider, date1.shiftedBy(-1), FramesFactory.getGCRF());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            provider.getAttitude(pvProvider, date4.shiftedBy(1), FramesFactory.getGCRF());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Add segment [d4 + 5, d4 + 10] 5s away from other segments and check sequence is not compact anymore
        provider.add(new AttitudeLawLeg(new BodyCenterPointing(), date4.shiftedBy(5), date4.shiftedBy(10)));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LEGS_SEQUENCE}
     * 
     * @testedMethod all set methods (first, last, subset, clear, etc.)
     * 
     * @description test all set methods of legs sequence
     * 
     * @input a leg sequence:
     *        - Body center pointing rotation on [AbsoluteDate.J2000_EPOCH; AbsoluteDate.J2000_EPOCH + 10s]
     *        - Sun pointing on [AbsoluteDate.J2000_EPOCH + 10s; AbsoluteDate.J2000_EPOCH + 20s]
     *        - Constant rotation on [AbsoluteDate.J2000_EPOCH + 20s; AbsoluteDate.J2000_EPOCH + 30s]
     * 
     * @output output of all set methods
     * 
     * @testPassCriteria result is as expected (functional test)
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void testSetLegsSequence() throws PatriusException {

        // Build profile
        final StrictAttitudeLegsSequence<AttitudeLeg> provider = new StrictAttitudeLegsSequence<>();
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AttitudeLeg leg1 = new AttitudeLawLeg(new BodyCenterPointing(), date1, date2);
        final AttitudeLeg leg2 =
            new AttitudeLawLeg(new SunPointing(Vector3D.PLUS_K, Vector3D.PLUS_I, new MeeusSun()), date2, date3);
        final AttitudeLeg leg3 =
            new AttitudeLawLeg(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY), date3, date4);
        provider.add(leg1);
        provider.add(leg2);
        provider.add(leg3);

        // Check set methods through various elementary manipulations
        Assert.assertTrue(this.isEqual(leg1, provider.previous(leg2)));
        Assert.assertTrue(this.isEqual(leg3, provider.next(leg2)));
        Assert.assertTrue(this.isEqual(leg2, provider.sub(leg2, leg3).first()));
        Assert.assertTrue(this.isEqual(leg2, provider.head(leg2).last()));
        Assert.assertTrue(this.isEqual(leg2, provider.tail(leg2).first()));
        Assert.assertEquals(3, provider.size());
        Assert.assertFalse(provider.isEmpty());
        Assert.assertTrue(provider.contains(leg1));
        Assert.assertTrue(this.isEqual(leg3, (AttitudeLeg) provider.toArray()[2]));
        final AttitudeLeg[] array = new AttitudeLeg[3];
        Assert.assertTrue(this.isEqual(leg3, provider.toArray(array)[2]));
        // Set them back through a list
        final List<AttitudeLeg> list = new ArrayList<>();
        list.add(leg1);
        list.add(leg2);
        list.add(leg3);
        provider.clear();
        provider.add(leg1);
        provider.add(leg2);
        provider.add(leg3);
        Assert.assertTrue(provider.containsAll(list));
        // Remove first element
        provider.remove(leg1);
        Assert.assertTrue(this.isEqual(leg2, provider.first()));
        // Remove 2nd and 3rd element
        provider.remove(leg2);
        provider.remove(leg3);
        Assert.assertEquals(0, provider.size());
        // Add 2nd and 3rd element
        provider.add(leg1);
        provider.add(leg2);
        provider.add(leg3);
        Assert.assertTrue(this.isEqual(leg1, provider.first()));
        provider.clear();
        Assert.assertEquals(0, provider.size());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LEGS_SEQUENCE}
     * 
     * @testedMethod toPrettyString
     * 
     * @description test the new toPrettyString method
     * 
     * @input a leg sequence:
     *        - Body center pointing rotation on [AbsoluteDate.J2000_EPOCH; AbsoluteDate.J2000_EPOCH + 10s]
     *        - Sun pointing on [AbsoluteDate.J2000_EPOCH + 10s; AbsoluteDate.J2000_EPOCH + 20s]
     *        - Constant rotation on [AbsoluteDate.J2000_EPOCH + 20s; AbsoluteDate.J2000_EPOCH + 30s]
     * 
     * @output string information
     * 
     * @testPassCriteria result is as expected (functional test)
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testToPrettyStringLegsSequence() throws PatriusException {

        // Build profile
        final StrictAttitudeLegsSequence<AttitudeLeg> provider = new StrictAttitudeLegsSequence<>();
        final String nature = "natureTest";
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AttitudeLeg leg1 = new AttitudeLawLeg(new BodyCenterPointing(), date1, date2, nature);
        final AttitudeLeg leg2 =
            new AttitudeLawLeg(new SunPointing(Vector3D.PLUS_K, Vector3D.PLUS_I, new MeeusSun()),
                date2, date3, nature);
        final AttitudeLeg leg3 =
            new AttitudeLawLeg(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY),
                date3, date4, nature);
        provider.add(leg1);
        provider.add(leg2);
        provider.add(leg3);

        final String expectedFirstSingleLegString = leg1.getTimeInterval() + " — "
                + leg1.getNature();

        String expectedLegsSequenceString = "Size : 3\n";
        expectedLegsSequenceString += "1 : " + leg1.getTimeInterval() + " — " + leg1.getNature() + "\n";
        expectedLegsSequenceString += "2 : " + leg2.getTimeInterval() + " — " + leg2.getNature() + "\n";
        expectedLegsSequenceString += "3 : " + leg3.getTimeInterval() + " — " + leg3.getNature() + "\n";

        Assert.assertTrue(leg1.toPrettyString().equals(expectedFirstSingleLegString));
        Assert.assertTrue(provider.toPrettyString().equals(expectedLegsSequenceString));

        // Test the specific case when the legs sequence is empty and the toPrettyString method is called
        final StrictAttitudeLegsSequence<AttitudeLeg> provider2 = new StrictAttitudeLegsSequence<>();
        final String expectedEmptyLegsSequenceString = "Empty legs sequence";
        Assert.assertTrue(provider2.toPrettyString().equals(expectedEmptyLegsSequenceString));

        final TimeSequence<Leg> timeSequence = new TimeSequence<Leg>(){
            @Override
            public <T> T[] toArray(final T[] a) {
                return null;
            }

            @Override
            public Object[] toArray() {
                return null;
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean retainAll(final Collection<?> c) {
                return false;
            }

            @Override
            public boolean removeAll(final Collection<?> c) {
                return false;
            }

            @Override
            public boolean remove(final Object o) {
                return false;
            }

            @Override
            public Iterator<Leg> iterator() {
                return new Iterator<Leg>(){

                    @Override
                    public Leg next() {
                        return null;
                    }

                    @Override
                    public boolean hasNext() {
                        return false;
                    }
                };
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public void clear() {
                // nothing to do
            }

            @Override
            public boolean addAll(final Collection<? extends Leg> c) {
                return false;
            }

            @Override
            public boolean add(final Leg e) {
                return false;
            }

            @Override
            public TimeSequence<Leg> tail(final Leg fromT) {
                return null;
            }

            @Override
            public TimeSequence<Leg> sub(final Leg fromT,
                                         final Leg toT) {
                return null;
            }

            @Override
            public Set<Leg> simultaneous(final TimeStamped t) {
                return null;
            }

            @Override
            public Leg previous(final Leg t) {
                return null;
            }

            @Override
            public Leg next(final Leg t) {
                return null;
            }

            @Override
            public Leg last(final TimeStamped t) {
                return null;
            }

            @Override
            public Leg last() {
                return null;
            }

            @Override
            public TimeSequence<Leg> head(final Leg toT) {
                return null;
            }

            @Override
            public Leg first(final TimeStamped t) {
                return null;
            }

            @Override
            public Leg first() {
                return null;
            }

            @Override
            public TimeSequence<Leg> copy() {
                return this;
            }
        };
        Assert.assertNotNull(timeSequence.toPrettyString());

        // New empty legs sequence
        final LegsSequence<Leg> emptyLegsSequence = new LegsSequence<Leg>(){

            @Override
            public Set<Leg> simultaneous(final TimeStamped t) {
                return null;
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public boolean contains(final Object o) {
                return false;
            }

            @Override
            public Iterator<Leg> iterator() {
                return null;
            }

            @Override
            public Object[] toArray() {
                return null;
            }

            @Override
            public <T> T[] toArray(final T[] a) {
                return null;
            }

            @Override
            public boolean add(final Leg e) {
                return false;
            }

            @Override
            public boolean remove(final Object o) {
                return false;
            }

            @Override
            public boolean containsAll(final Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(final Collection<? extends Leg> c) {
                return false;
            }

            @Override
            public boolean removeAll(final Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(final Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {
                // nothing to do
            }

            @Override
            public Leg current(final TimeStamped t) {
                return null;
            }

            @Override
            public Leg first() {
                return null;
            }

            @Override
            public Leg last() {
                return null;
            }

            @Override
            public Leg first(final TimeStamped t) {
                return null;
            }

            @Override
            public Leg last(final TimeStamped t) {
                return null;
            }

            @Override
            public Set<Leg> simultaneous(final Leg leg) {
                return null;
            }

            @Override
            public Leg previous(final Leg leg) {
                return null;
            }

            @Override
            public Leg next(final Leg leg) {
                return null;
            }

            @Override
            public LegsSequence<Leg> head(final Leg toLeg) {
                return null;
            }

            @Override
            public LegsSequence<Leg> tail(final Leg fromLeg) {
                return null;
            }

            @Override
            public LegsSequence<Leg> sub(final Leg fromLeg,
                                         final Leg toLeg) {
                return null;
            }

            @Override
            public LegsSequence<Leg> sub(final AbsoluteDate fromT,
                                         final AbsoluteDate toT,
                                         final boolean strict) {
                return null;
            }

            @Override
            public LegsSequence<Leg> sub(final AbsoluteDateInterval interval,
                                         final boolean strict) {
                return null;
            }

            @Override
            public LegsSequence<Leg> head(final AbsoluteDate toT,
                                          final boolean strict) {
                return null;
            }

            @Override
            public LegsSequence<Leg> tail(final AbsoluteDate fromT,
                                          final boolean strict) {
                return null;
            }

            @Override
            public boolean isEmpty(final AbsoluteDate date,
                                   final AbsoluteDate end) {
                return false;
            }

            @Override
            public AbsoluteDateInterval getTimeInterval() {
                return null;
            }

            @Override
            public LegsSequence<Leg> copy() {
                return null;
            }

            @Override
            public LegsSequence<Leg> copy(final AbsoluteDateInterval newInterval,
                                          final boolean strict) {
                return null;
            }
        };
        Assert.assertNotNull(emptyLegsSequence.toPrettyString());
        // New legs sequence
        final LegsSequence<Leg> legsSequence = new LegsSequence<Leg>(){

            @Override
            public Set<Leg> simultaneous(final TimeStamped t) {
                return null;
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(final Object o) {
                return false;
            }

            @Override
            public Iterator<Leg> iterator() {
                return new Iterator<Leg>(){

                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public Leg next() {
                        return null;
                    }
                };
            }

            @Override
            public Object[] toArray() {
                return null;
            }

            @Override
            public <T> T[] toArray(final T[] a) {
                return null;
            }

            @Override
            public boolean add(final Leg e) {
                return false;
            }

            @Override
            public boolean remove(final Object o) {
                return false;
            }

            @Override
            public boolean containsAll(final Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(final Collection<? extends Leg> c) {
                return false;
            }

            @Override
            public boolean removeAll(final Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(final Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {
                // nothing to do
            }

            @Override
            public Leg current(final TimeStamped t) {
                return null;
            }

            @Override
            public Leg first() {
                return null;
            }

            @Override
            public Leg last() {
                return null;
            }

            @Override
            public Leg first(final TimeStamped t) {
                return null;
            }

            @Override
            public Leg last(final TimeStamped t) {
                return null;
            }

            @Override
            public Set<Leg> simultaneous(final Leg leg) {
                return null;
            }

            @Override
            public Leg previous(final Leg leg) {
                return null;
            }

            @Override
            public Leg next(final Leg leg) {
                return null;
            }

            @Override
            public LegsSequence<Leg> head(final Leg toLeg) {
                return null;
            }

            @Override
            public LegsSequence<Leg> tail(final Leg fromLeg) {
                return null;
            }

            @Override
            public LegsSequence<Leg> sub(final Leg fromLeg,
                                         final Leg toLeg) {
                return null;
            }

            @Override
            public LegsSequence<Leg> sub(final AbsoluteDate fromT,
                                         final AbsoluteDate toT,
                                         final boolean strict) {
                return null;
            }

            @Override
            public LegsSequence<Leg> sub(final AbsoluteDateInterval interval,
                                         final boolean strict) {
                return null;
            }

            @Override
            public LegsSequence<Leg> head(final AbsoluteDate toT,
                                          final boolean strict) {
                return null;
            }

            @Override
            public LegsSequence<Leg> tail(final AbsoluteDate fromT,
                                          final boolean strict) {
                return null;
            }

            @Override
            public boolean isEmpty(final AbsoluteDate date,
                                   final AbsoluteDate end) {
                return false;
            }

            @Override
            public AbsoluteDateInterval getTimeInterval() {
                return null;
            }

            @Override
            public LegsSequence<Leg> copy() {
                return null;
            }

            @Override
            public LegsSequence<Leg> copy(final AbsoluteDateInterval newInterval,
                                          final boolean strict) {
                return this;
            }
        };
        Assert.assertNotNull(legsSequence.toPrettyString());
        Assert.assertNotNull(legsSequence.copy(new AbsoluteDateInterval(date1, date2)));
    }

    /**
     * Check two segments are equal or not
     * 
     * @param leg1 1st leg
     * @param leg2 2nd leg
     * @return true if equal
     * @throws PatriusException thrown if failed to retrieve time interval
     */
    private boolean isEqual(final AttitudeLeg leg1, final AttitudeLeg leg2) throws PatriusException {
        if (leg1 == null) {
            return leg2 == null;
        }
        return leg1.getTimeInterval().equals(leg2.getTimeInterval());
    }
}
