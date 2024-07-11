/**
 *
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
 *
 * @history 20/01/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3144:10/05/2022:[PATRIUS] Classe TempDirectory en double 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:416:12/02/2015:Changed EcksteinHechlerPropagator constructor signature
 * VERSION::DM:368:24/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.validation.events;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.validationTool.TemporaryDirectory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * This main aims at testing the Orekit event detection process when using a propagator.
 * </p>
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: EventDetectionValidation.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class EventDetectionValidation {

    /** Info string. */
    private static final String SEMICO = ";";
    /** Info string. */
    private static final String EXECUTION_TIME_MS = "Execution time (ms)";
    /** Info string. */
    private static final String PRECISION = "Precision";
    /** Info string. */
    private static final String CONVERGENCE = "Convergence";
    /** Info string. */
    private static final String MAX_CHECK_INTERVAL = "Max check interval";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Event detection parametrisation
         * 
         * @featureDescription how to deal with the event detection
         *                     parametrisation to reach the required precision without
         *                     deteriorate the performances?
         * 
         * @coveredRequirements DV-EVT_63, DV-PROPAG_130
         */
        PARAMETRIZATION_PRECISION
    }

    /** 3 dots. */
    private static final String TDOTS = "...";

    /** Directory of the output files. */
    private static final String outputDirectory =
        TemporaryDirectory.getTemporaryDirectory("pdb.misc.results", "eventDetectionValidation");

    /** Lower end boundary of the integration interval. */
    private static AbsoluteDate iDate = new AbsoluteDate(2000, 1, 10,
        TimeScalesFactory.getTT());

    /** Upper end boundary of the integration interval. */
    private static AbsoluteDate fDate = iDate.shiftedBy(100);

    /** Number of trials. */
    private static int numberOfTrials = 10;

    /** Default parametrisation : max check interval. */
    private static double defaultMaxCheckInterval = 0.001;

    /** Default parametrisation : default convergence. */
    private static double defaultConvergence = 0.01;

    /** Equatorial radius. */
    private static double re = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;

    /** Semi-major axis. */
    private static double a = re + 900000;

    /** Generic orbit. */
    private static Orbit orbit;

    /** The different propagators used in the test. */
    private enum PropagatorType {
        /** Numerical propagator with Runge Kutta 4. */
        RK4,
        /** Numerical propagator with GBS. */
        GBS,
        /** Numerical propagator with Dormand Prince 853. */
        DOP853,
        /** Keplerian propagator. */
        KEP,
        /** Echstein-Hechler propagator. */
        EHP
    }

    /**
     * Private constructor.
     */
    private EventDetectionValidation() {
    }

    /**
     * <p>
     * Main method which launch the propagation of a generic orbit with numerical propagators (using Runge Kutta,
     * Dormand Prince 853 or Gragg Bulirsch Stoer integrator), Keplerian propagator and Eckstein Hechler propagator. The
     * propagator has been supplied with 10 mock events at 10 different dates. The purpose of this test is to analyse
     * the detection parametrization. This parametrization concerns the maximum interval length where events are
     * checked, the convergence (i.e. the precision of the detection process) and a maximum number of iteration to find
     * an event. We will try different maximum check interval lengths as well as different convergence values. The
     * generated files are stored in the temporary directory : 1 file per integrator.
     * </p>
     * 
     * @param args
     *        unused
     * 
     * @throws PatriusException
     *         Orekit exception
     * @throws IOException
     *         i/o exception
     * 
     * @since 1.0
     */
    public static void main(final String[] args) throws
                                                PatriusException, IOException {
        System.out.println("DEBUT " + EventDetectionValidation.class.getName());
        Utils.setDataRoot("regular-dataCNES-2003");

        // add the package as containing directory for this test
        final String directory = outputDirectory + File.separator + "detection";

        // build a File object for this directory
        final File dir = new File(directory);
        dir.mkdirs();

        final Orbit orb = new KeplerianOrbit(a, 0, 0.1, 0, 0,
            0, PositionAngle.MEAN, FramesFactory.getGCRF(), iDate,
            Constants.EGM96_EARTH_MU);

        // Propagators map
        Map<PropagatorType, Propagator> mapIntegrators;
        // buffered writers associated to the integrators of the previous map
        Map<PropagatorType, BufferedWriter> mapWriters;

        // Time events
        final int n = 10;
        final List<AbsoluteDate> times = new ArrayList<AbsoluteDate>();
        for (int i = 0; i < n; i++) {
            times.add(iDate.shiftedBy(i * 9.99876543));
        }
        // Different tried convergences
        final double[] convergences = { 0.1e-4, 0.5e-4, 1e-4, 5e-4, 1e-3, 5e-3, 1e-2, 0.05, 0.1, 0.11, 0.2, 0.5, 0.7,
            1, 1.5, 1.9, 2, 2.5, 5 };

        // Different max check interval lengths
        final double[] maxCheckIntervals = { 1e-4, 5 * 1e-4, 1e-3, 5 * 1e-3, 1e-2, 0.05, 0.07, 0.1, 0.2, 0.5, 1, 1.5,
            3 };

        // creation of the writers relative to the tested propagators
        mapWriters = getPropagatorWriterList(dir);

        final Set<PropagatorType> types = mapWriters.keySet();

        // this integrator parametrization enables a precision of 1e-4
        mapIntegrators = getPropagatorList(0.2, 0.01, 2, 1e-4, orb, null);

        for (final double convergence2 : convergences) {
            System.out.println("CVG " + convergence2 + TDOTS);
            mapIntegrators = getPropagatorList(0.2, 0.01, 2, 1e-4, orb, null);
            calculate(mapIntegrators, times, defaultMaxCheckInterval,
                convergence2, mapWriters);
        }

        for (final double maxCheckInterval : maxCheckIntervals) {
            System.out.println("MAXCHK " + maxCheckInterval + TDOTS);
            mapIntegrators = getPropagatorList(0.2, 0.01, 2, 1e-4, orb, null);
            calculate(mapIntegrators, times, maxCheckInterval,
                defaultConvergence, mapWriters);
        }

        for (final PropagatorType integ : types) {
            mapWriters.get(integ).close();
        }

        System.out.println("done !");
    }

    /**
     * @description Run the simulation and write in several files
     *              the results relative to the detection event process.
     * 
     * @param mapPropagators
     *        : propagators map
     * @param times
     *        : list of the event times
     * @param maxCheckInterval
     *        : length of the maximum check interval
     * @param convergence
     *        : convergence value
     * @param mapWriters
     *        : propagator buffered writers
     * @throws IOException
     *         IO exception
     * @throws PatriusException
     *         orekit exception
     * 
     * @since 1.0
     */
    private static void calculate(
                                  final Map<PropagatorType, Propagator> mapPropagators,
                                  final List<AbsoluteDate> times, final double maxCheckInterval,
                                  final double convergence,
                                  final Map<PropagatorType, BufferedWriter> mapWriters)
                                                                                       throws IOException,
                                                                                       PatriusException {

        // Integration execution time
        long start;
        long duree;

        // Gap between the theoretical event time and the computed one
        double gap;
        double maxGap;

        final Set<PropagatorType> propagators = mapPropagators.keySet();

        Propagator prop;

        String result;

        for (final PropagatorType propagator : propagators) {
            System.out.println("PROPAG " + propagator);
            gap = -1;
            maxGap = -1;

            final List<EventDetectorMock> eventList = new ArrayList<EventDetectorMock>();
            for (int i = 1; i < times.size(); i++) {
                eventList.add(new EventDetectorMock(times.get(i), maxCheckInterval, convergence));
            }

            prop = mapPropagators.get(propagator);
            orbit = new KeplerianOrbit(a, 0, 0.1, 0, 0,
                0, PositionAngle.MEAN, FramesFactory.getGCRF(), iDate,
                Constants.EGM96_EARTH_MU);
            prop.resetInitialState(new SpacecraftState(orbit));
            prop.clearEventsDetectors();

            // tic
            for (int j = 0; j < eventList.size(); j++) {
                prop.addEventDetector(eventList.get(j));
            }
            start = System.currentTimeMillis();
            for (int i = 0; i < numberOfTrials; i++) {
                prop.propagate(fDate);
            }

            // toc
            duree = (System.currentTimeMillis() - start) / numberOfTrials;

            for (int i = 0; i < eventList.size(); i++) {
                gap = MathLib.abs(eventList.get(i).getTheoreticalTime()
                    .durationFrom(eventList.get(i).getEventTime()));
                // System.out.println("GAP " + gap);
                if (gap > maxGap) {
                    maxGap = gap;
                }
            }
            prop.resetInitialState(new SpacecraftState(orbit));

            result = maxCheckInterval + SEMICO + convergence + SEMICO + maxGap + SEMICO + duree;
            System.out.println("MAXGAP " + maxGap + " DUREE " + duree);
            mapWriters.get(propagator).write(result.replace('.', ','));
            mapWriters.get(propagator).newLine();
        }
    }

    /**
     * @description Create the propagators list.
     * 
     * @param h
     *        : step size of the fixed step integrators
     * @param hmin
     *        : min step size of adaptive step integrators
     * @param hmax
     *        : max step size of adaptive step integrators
     * @param tol
     *        : absolute tolerance for adaptive step integrators
     * @param orb
     *        : generic orbit
     * @param massProvider
     *        : mass provider
     * @return list of the propagators
     * @throws PropagationException
     *         propagation exception
     * 
     * @since 1.0
     */
    private static
            Map<PropagatorType, Propagator>
            getPropagatorList(
                              final double h, final double hmin, final double hmax,
                              final double tol, final Orbit orb, final MassProvider massProvider)
                                                                                                 throws PropagationException {
        final Map<PropagatorType, Propagator> map = new TreeMap<PropagatorType, Propagator>();

        final FirstOrderIntegrator integRK4 = new ClassicalRungeKuttaIntegrator(h);
        final NumericalPropagator propRK4 = new NumericalPropagator(integRK4);
        map.put(PropagatorType.RK4, propRK4);
        final FirstOrderIntegrator integGBS = new GraggBulirschStoerIntegrator(hmin, hmax, tol, 1);
        final NumericalPropagator propGBS = new NumericalPropagator(integGBS);
        map.put(PropagatorType.GBS, propGBS);
        final FirstOrderIntegrator integDOP = new DormandPrince853Integrator(hmin, hmax, tol, 1);
        final NumericalPropagator propDOP = new NumericalPropagator(integDOP);
        map.put(PropagatorType.DOP853, propDOP);
        final KeplerianPropagator propKEP = new KeplerianPropagator(orb);
        map.put(PropagatorType.KEP, propKEP);
        final EcksteinHechlerPropagator propEHP = new EcksteinHechlerPropagator(orb,
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS,
            Constants.EGM96_EARTH_MU, orbit.getFrame(), Constants.EGM96_EARTH_C20,
            Constants.EGM96_EARTH_C30, Constants.EGM96_EARTH_C40,
            Constants.EGM96_EARTH_C50, Constants.EGM96_EARTH_C60,
            massProvider, ParametersType.OSCULATING);
        map.put(PropagatorType.EHP, propEHP);

        return map;
    }

    /**
     * Create the list of the buffered writers relative to a list of propagators
     * (1 buffered writer per propagator).
     * 
     * @param dir
     *        directory file
     * @return the list of the buffered writers
     * @throws IOException
     *         IO exception
     * 
     * @since 1.0
     */
    private static Map<PropagatorType, BufferedWriter> getPropagatorWriterList(final File dir) throws IOException {
        // String containing the paths of output files
        final String filePathrk4 = dir.getPath() + File.separator + "report_RK4.csv";
        final String filePathgbs = dir.getPath() + File.separator + "report_GBS.csv";
        final String filePathdop = dir.getPath() + File.separator + "report_DOP.csv";
        final String filePathkep = dir.getPath() + File.separator + "report_KEP.csv";
        final String filePathehp = dir.getPath() + File.separator + "report_EHP.csv";

        // Associate a log file to paths
        final File logrk4 = new File(filePathrk4);
        final File loggbs = new File(filePathgbs);
        final File logdop = new File(filePathdop);
        final File logkep = new File(filePathkep);
        final File logehp = new File(filePathehp);

        // Get a writer for this log
        final BufferedWriter writerRK4 = new BufferedWriter(new FileWriter(logrk4));
        final BufferedWriter writerGBS = new BufferedWriter(new FileWriter(loggbs));
        final BufferedWriter writerDOP = new BufferedWriter(new FileWriter(logdop));
        final BufferedWriter writerKEP = new BufferedWriter(new FileWriter(logkep));
        final BufferedWriter writerEHP = new BufferedWriter(new FileWriter(logehp));

        // Set the output files first lines
        // Runge-Kutta 4
        writerRK4.write("Runge Kutta");
        writerRK4.newLine();
        writerRK4.newLine();
        writerRK4.write(MAX_CHECK_INTERVAL + SEMICO + CONVERGENCE + SEMICO + PRECISION + SEMICO
            + EXECUTION_TIME_MS);
        writerRK4.newLine();
        // GBS
        writerGBS.write("GBS");
        writerGBS.newLine();
        writerGBS.newLine();
        writerGBS.write(MAX_CHECK_INTERVAL + SEMICO + CONVERGENCE + SEMICO + PRECISION + SEMICO
            + EXECUTION_TIME_MS);
        writerGBS.newLine();
        // DOP 853
        writerDOP.write("DOP853");
        writerDOP.newLine();
        writerDOP.newLine();
        writerDOP.write(MAX_CHECK_INTERVAL + SEMICO + CONVERGENCE + SEMICO + PRECISION + SEMICO
            + EXECUTION_TIME_MS);
        writerDOP.newLine();
        // Keplerian propagator
        writerKEP.write("Keplerian propagator");
        writerKEP.newLine();
        writerKEP.newLine();
        writerKEP.write(MAX_CHECK_INTERVAL + SEMICO + CONVERGENCE + SEMICO + PRECISION + SEMICO
            + EXECUTION_TIME_MS);
        writerKEP.newLine();
        // Echstein-Hechler propagator
        writerEHP.write("Echstein-Hechler propagator");
        writerEHP.newLine();
        writerEHP.newLine();
        writerEHP.write(MAX_CHECK_INTERVAL + SEMICO + CONVERGENCE + SEMICO + PRECISION + SEMICO
            + EXECUTION_TIME_MS);
        writerEHP.newLine();

        final Map<PropagatorType, BufferedWriter> map = new TreeMap<PropagatorType, BufferedWriter>();
        // Associate a key to propagators
        map.put(PropagatorType.RK4, writerRK4);
        map.put(PropagatorType.GBS, writerGBS);
        map.put(PropagatorType.DOP853, writerDOP);
        map.put(PropagatorType.KEP, writerKEP);
        map.put(PropagatorType.EHP, writerEHP);

        return map;
    }
}
