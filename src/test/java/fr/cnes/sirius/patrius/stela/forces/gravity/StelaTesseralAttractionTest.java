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
 * HISTORY
 * VERSION:4.9:FA:FA-3126:10/05/2022:[PATRIUS] Imports obsoletes suite a suppression de reflexion Java dans Patrius 
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:update with renamed classes
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.gravity;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsReader;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980Entry;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980History;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980HistoryLoader;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Test class for {@link StelaTesseralAttraction}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaTesseralAttractionTest {

    // =================================== Input data ===================================

    /** Integration step. */
    private final double integrationStep = 86400;

    /** Minimum number of integration step period before taking into account tesseral harmonics. */
    private final int nbIntegrationStep = 5;

    /** Date. */
    private AbsoluteDate date;

    // =================================== Tolerance ===================================

    /** Tolerance fc, fs. */
    private static final double tol_fcfs = 1E-14;
    /** Tolerance ec. */
    private static final double tol_ec = 1E-14;
    /** Tolerance delta-e. */
    private static final double tol_deltae = 1E-14;
    /** Tolerance Taylor coefficients. */
    private static final double[] tol_taylor = new double[] { 1E-14, 1E-10, 1E-7 };
    /** Tolerance Taylor derivative coefficients. */
    private static final double[] tol_diffTaylor = new double[] { 1E-14, 1E-3, 1E-3 };
    /** Tolerance inclination function. */
    private static final double tol_f = 1E-15;
    /** Tolerance eccentricity function. */
    private static final double tol_g = 1E-15;

    // =================================== Reference results ===================================

    /** Expected quads. */
    private final int[][] expected_quads = { { 2, 1, 0, -1 }, { 2, 1, 1, 1 }, { 2, 2, 0, 0 }, { 2, 2, 1, 2 },
        { 3, 1, 0, -2 }, { 3, 1, 1, 0 }, { 3, 1, 2, 2 }, { 3, 2, 0, -1 }, { 3, 2, 1, 1 }, { 3, 3, 0, 0 },
        { 3, 3, 1, 2 }, { 4, 1, 1, -1 }, { 4, 1, 2, 1 }, { 4, 2, 0, -2 }, { 4, 2, 1, 0 }, { 4, 2, 2, 2 },
        { 4, 3, 0, -1 }, { 4, 3, 1, 1 }, { 4, 4, 0, 0 }, { 4, 4, 1, 2 }, { 5, 1, 1, -2 }, { 5, 1, 2, 0 },
        { 5, 1, 3, 2 }, { 5, 2, 1, -1 }, { 5, 2, 2, 1 }, { 5, 3, 0, -2 }, { 5, 3, 1, 0 }, { 5, 3, 2, 2 },
        { 5, 4, 0, -1 }, { 5, 4, 1, 1 }, { 5, 5, 0, 0 }, { 5, 5, 1, 2 }, { 6, 1, 2, -1 }, { 6, 1, 3, 1 },
        { 6, 2, 1, -2 }, { 6, 2, 2, 0 }, { 6, 2, 3, 2 }, { 6, 3, 1, -1 }, { 6, 3, 2, 1 }, { 6, 4, 0, -2 },
        { 6, 4, 1, 0 }, { 6, 4, 2, 2 }, { 6, 5, 0, -1 }, { 6, 5, 1, 1 }, { 6, 6, 0, 0 }, { 6, 6, 1, 2 },
        { 7, 1, 2, -2 }, { 7, 1, 3, 0 }, { 7, 1, 4, 2 }, { 7, 2, 2, -1 }, { 7, 2, 3, 1 }, { 7, 3, 1, -2 },
        { 7, 3, 2, 0 }, { 7, 3, 3, 2 }, { 7, 4, 1, -1 }, { 7, 4, 2, 1 }, { 7, 5, 0, -2 }, { 7, 5, 1, 0 },
        { 7, 5, 2, 2 }, { 7, 6, 0, -1 }, { 7, 6, 1, 1 }, { 7, 7, 0, 0 }, { 7, 7, 1, 2 } };

    /** Expected central eccentricity and &delta:e. */
    private final double[][] expected_eccentricity = { { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 },
        { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 },
        { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 },
        { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 },
        { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 },
        { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 },
        { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 },
        { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 },
        { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 },
        { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 },
        { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 }, { 0.001, 0.02 } };

    /** Expected fc and fs data. */
    private final double[][] expected_fcfs1 = { { -1.71041723998733E-09, -2.10257928166259E-10 },
        { -1.71041723998733E-09, -2.10257928166259E-10 }, { 1.57457394852328E-06, -9.03874613903651E-07 },
        { 1.57457394852328E-06, -9.03874613903651E-07 }, { 2.19298008521395E-06, 2.67994204424007E-07 },
        { 2.19298008521395E-06, 2.67994204424007E-07 }, { 2.19298008521395E-06, 2.67994204424007E-07 },
        { 2.11430029406192E-07, 3.09057987103261E-07 }, { 2.11430029406192E-07, 3.09057987103261E-07 },
        { 1.00573583503370E-07, 1.97198191638981E-07 }, { 1.00573583503370E-07, 1.97198191638981E-07 },
        { 4.48982549284929E-07, -5.08819881050582E-07 }, { 4.48982549284929E-07, -5.08819881050582E-07 },
        { 7.83515989043412E-08, 1.48150645197125E-07 }, { 7.83515989043412E-08, 1.48150645197125E-07 },
        { 7.83515989043412E-08, 1.48150645197125E-07 }, { 1.20114164321369E-08, 5.92193052150293E-08 },
        { 1.20114164321369E-08, 5.92193052150293E-08 }, { -3.98275086528604E-09, 6.52486696015096E-09 },
        { -3.98275086528604E-09, 6.52486696015096E-09 }, { -5.35159907035942E-08, -8.05589565010746E-08 },
        { -5.35159907035942E-08, -8.05589565010746E-08 }, { -5.35159907035942E-08, -8.05589565010746E-08 },
        { 5.23066175268146E-08, 1.05539452567502E-07 }, { 5.23066175268146E-08, 1.05539452567502E-07 },
        { -1.49153834399890E-08, -7.09190205479633E-09 }, { -1.49153834399890E-08, -7.09190205479633E-09 },
        { -1.49153834399890E-08, -7.09190205479633E-09 }, { -3.87499729475374E-10, -2.29889994258429E-09 },
        { -3.87499729475374E-10, -2.29889994258429E-09 }, { 4.30452849401142E-10, -1.64806063974034E-09 },
        { 4.30452849401142E-10, -1.64806063974034E-09 }, { -2.04969401077996E-08, -5.95498987063517E-08 },
        { -2.04969401077996E-08, -5.95498987063517E-08 }, { 6.14809278786964E-09, -4.65045896026979E-08 },
        { 6.14809278786964E-09, -4.65045896026979E-08 }, { 6.14809278786964E-09, -4.65045896026979E-08 },
        { -1.83142783342438E-10, 1.19337231152767E-09 }, { -1.83142783342438E-10, 1.19337231152767E-09 },
        { -3.27180887553721E-10, -1.78440986766777E-09 }, { -3.27180887553721E-10, -1.78440986766777E-09 },
        { -3.27180887553721E-10, -1.78440986766777E-09 }, { 4.32949214597265E-10, -2.15596998175908E-10 },
        { 4.32949214597265E-10, -2.15596998175908E-10 }, { 2.20166967676734E-12, -5.52898532225177E-11 },
        { 2.20166967676734E-12, -5.52898532225177E-11 }, { 2.04579232483885E-07, 6.88682995756525E-08 },
        { 2.04579232483885E-07, 6.88682995756525E-08 }, { 2.04579232483885E-07, 6.88682995756525E-08 },
        { -9.24882109962778E-09, 3.29236456008551E-08 }, { -9.24882109962778E-09, 3.29236456008551E-08 },
        { 3.51027844109090E-09, -3.07292066161126E-09 }, { 3.51027844109090E-09, -3.07292066161126E-09 },
        { 3.51027844109090E-09, -3.07292066161126E-09 }, { 2.63595579307789E-10, -5.84435766316767E-10 },
        { 2.63595579307789E-10, -5.84435766316767E-10 }, { 6.65483718822662E-13, 6.40132886359319E-12 },
        { 6.65483718822662E-13, 6.40132886359319E-12 }, { 6.65483718822662E-13, 6.40132886359319E-12 },
        { -1.05366085647820E-11, -2.49019995881171E-11 }, { -1.05366085647820E-11, -2.49019995881171E-11 },
        { 2.88239698529975E-14, 4.49551039511791E-13 }, { 2.88239698529975E-14, 4.49551039511791E-13 } };

    /** Expected Taylor coefficients. */
    private final double[][] expected_Taylor = { { -4.99999937500013E-01, 1.24999895767530E-04, 6.24999088039146E-02 },
        { 1.50000168750203E+00, 3.37501631286762E-03, 1.68751427365165E+00 },
        { 9.99997500000812E-01, -4.99999349995450E-03, -2.49999431239933E+00 },
        { 2.25000175000220E+00, 3.50001762572382E-03, 1.75001542257469E+00 },
        { 1.25000020833351E-01, 4.16668099328942E-05, 2.08334586665470E-02 },
        { 1.00000200000373E+00, 4.00002987532843E-03, 2.00002614092475E+00 },
        { 1.37500306250509E+00, 6.12504079477993E-03, 3.06253569548608E+00 },
        { -9.99998750000146E-01, 2.49999883311646E-03, 1.24999897926913E+00 },
        { 3.00000275000510E+00, 5.50004083410548E-03, 2.75003572869358E+00 },
        { 9.99994000006609E-01, -1.19999471249188E-02, -5.99995373423700E+00 },
        { 6.62500243750687E+00, 4.87505501078899E-03, 2.43754813444851E+00 },
        { 5.00002062504856E-01, 4.12503885460768E-03, 2.06253399770428E+00 },
        { 2.50000843751897E+00, 1.68751517723997E-02, 8.43763280000686E+00 },
        { 4.99999666666666E-01, -6.66666666482296E-04, -3.33333333379925E-01 },
        { 1.00000100000406E+00, 2.00003250050428E-03, 1.00002843761437E+00 },
        { 5.00001291669276E+00, 2.58335420859623E-02, 1.29168493252507E+01 },
        { -1.49999531250307E+00, 9.37497543718190E-03, 4.68747850812522E+00 },
        { 4.49999981250752E+00, -3.74939811198515E-04, -1.87447336319479E-01 },
        { 9.99989000024875E-01, -2.19998010004274E-02, -1.09998258756571E+01 },
        { 1.32499925416819E+01, -1.49165445373355E-02, -7.45822647107985E+00 },
        { 3.75001750004991E-01, 3.50003993021541E-03, 1.75003493890768E+00 },
        { 1.00000650002185E+00, 1.30001748769315E-02, 6.50015301706385E+00 },
        { 3.62501608337733E+00, 3.21670186893552E-02, 1.60836413523046E+01 },
        { 1.50000399993863E-06, 3.00003200029632E-03, 1.50002800035768E+00 },
        { 4.00001450003891E+00, 2.90003113367909E-02, 1.45002724187825E+01 },
        { 1.12499775000085E+00, -4.49999317919669E-03, -2.24999403242165E+00 },
        { 9.99998500004734E-01, -2.99996212455022E-03, -1.49996685921971E+00 },
        { 1.08750267500630E+01, 5.35005042188174E-02, 2.67504411883834E+01 },
        { -1.99998850001683E+00, 2.29998653331620E-02, 1.14998821671985E+01 },
        { 5.99998950001550E+00, -2.09998759990170E-02, -1.04998915002596E+01 },
        { 9.99982500066484E-01, -3.49994681276277E-02, -1.74995346122197E+01 },
        { 2.21249557500517E+01, -8.84995863579262E-02, -4.42496380692602E+01 },
        { 1.50001006253708E+00, 2.01252966495202E-02, 1.00627595681634E+01 },
        { 3.50002318758467E+00, 4.63756774040646E-02, 2.31880927286098E+01 },
        { 2.50001000003526E-01, 2.00002820874289E-03, 1.00002468264581E+00 },
        { 1.00000650002618E+00, 1.30002095026782E-02, 6.50018331493917E+00 },
        { 8.75004433347348E+00, 8.86677878479957E-02, 4.43343143636809E+01 },
        { -4.99998062497617E-01, 3.87501906251586E-03, 1.93751667992847E+00 },
        { 5.50001731255541E+00, 3.46254433187809E-02, 1.73128879019657E+01 },
        { 1.99999233334104E+00, -1.53332716660292E-02, -7.66661270856782E+00 },
        { 9.99994500011000E-01, -1.09999119997095E-02, -5.49992300002965E+00 },
        { 1.90000350001041E+01, 7.00008328458068E-02, 3.50007287330811E+01 },
        { -2.49997718755767E+00, 4.56245386468712E-02, 2.28120963170486E+01 },
        { 7.49996843755155E+00, -6.31245876028252E-02, -3.15621391542286E+01 },
        { 9.99974500145687E-01, -5.09988345098188E-02, -2.54989801973803E+01 },
        { 3.32498690002047E+01, -2.61998361864357E-01, -1.30998566639561E+02 },
        { 1.62501197921576E+00, 2.39587260966178E-02, 1.19795103346032E+01 },
        { 1.00001300007086E+00, 2.60005668835105E-02, 1.30004960234852E+01 },
        { 6.87505010436966E+00, 1.00209957348162E-01, 5.01055876789280E+01 },
        { 1.00000825003560E+00, 1.65002848374617E-02, 8.25024923301053E+00 },
        { 5.00003675015352E+00, 7.35012281842806E-02, 3.67510746612786E+01 },
        { 3.75000187502276E-01, 3.75018211290489E-04, 1.87515934696458E-01 },
        { 1.00000500002573E+00, 1.00002058780468E-02, 5.00018014348846E+00 },
        { 1.61250845627957E+01, 1.69127366072885E-01, 8.45645703098085E+01 },
        { -9.99995500002167E-01, 8.99998266640222E-03, 4.49998483337265E+00 },
        { 7.00001350006416E+00, 2.70005133415906E-02, 1.35004491728096E+01 },
        { 3.12498072920194E+00, -3.85413844392612E-02, -1.92705863863995E+01 },
        { 9.99989000031484E-01, -2.19997481255007E-02, -1.09997796101102E+01 },
        { 2.93750213543103E+01, 4.27094823791662E-02, 2.13551720733562E+01 },
        { -2.99996025015281E+00, 7.94987775063482E-02, 3.97489303201048E+01 },
        { 8.99993325016294E+00, -1.33498696500922E-01, -6.67488594405796E+01 },
        { 9.99965000280108E-01, -6.99977591527844E-02, -3.49980392614912E+01 },
        { 4.66247024798540E+01, -5.95036167595708E-01, -2.97516021674937E+02 } };

    /** Expected Taylor derivative coefficients. */
    private final double[][] expected_TaylorDiff = {
        { 6.2499973958310100E-02, -5.2083519514606300E-05, -2.6041829661455000E-02 },
        { 1.6875040781319800E+00, 8.1563058950129200E-03, 4.0781739082396100E+00 },
        { -2.4999983750003600E+00, 3.2499970830102100E-03, 1.6249974477133800E+00 },
        { 1.7500044062573800E+00, 8.8125591005683200E-03, 4.4063017131668000E+00 },
        { 2.0833369140663300E-02, 7.1614889816939900E-05, 3.5807559844308600E-02 },
        { 2.0000074687673000E+00, 1.4937638459722100E-02, 7.4688711524384300E+00 },
        { 3.0625101985899900E+00, 2.0397313674536100E-02, 1.0198723684240400E+01 },
        { 1.2499997083335700E+00, -5.8333141628175100E-04, -2.9166498971555600E-01 },
        { 2.7500102083550300E+00, 2.0416840293302400E-02, 1.0208485256901700E+01 },
        { -5.9999867812558600E+00, 2.6437453124028500E-02, 1.3218708984741300E+01 },
        { 2.4375137519792900E+00, 2.7504115587495500E-02, 1.3752136297062700E+01 },
        { 2.0625097135692100E+00, 1.9427303685759500E-02, 9.7137344750031100E+00 },
        { 8.4375379428129900E+00, 7.5886253934953600E-02, 3.7943440942100800E+01 },
        { -3.3333333333342500E-01, -7.3352435236984000E-10, -6.4154237477964600E-07 },
        { 1.0000081250231600E+00, 1.6250185335242500E-02, 8.1251621685263200E+00 },
        { 1.2916718854301500E+01, 1.0437607942748100E-01, 5.2188444502831900E+01 },
        { 4.6874938593768300E+00, -1.2281235338917800E-02, -6.1406121742990600E+00 },
        { -1.8748495309169500E-01, 3.0094016450404800E-02, 1.5047108146204400E+01 },
        { -1.0999950250054500E+01, 9.9499563332550100E-02, 4.9749617919481100E+01 },
        { -7.4583028020408700E+00, 6.1062839801007100E-02, 3.0531547342338400E+01 },
        { 1.7500099824552100E+00, 1.9965110498842900E-02, 9.9826552801829300E+00 },
        { 6.5000437189110300E+00, 8.7438788286320300E-02, 4.3719877250936100E+01 },
        { 1.6083421338175900E+01, 1.7601139683698300E-01, 8.8006554262776100E+01 },
        { 1.5000080000275600E+00, 1.6000220502632100E-02, 8.0001929397832600E+00 },
        { 1.4500077833585200E+01, 1.5566868177252500E-01, 7.7835096551126400E+01 },
        { -2.2499982949223900E+00, 3.4101520725027200E-03, 1.7050744707258700E+00 },
        { -1.4999905312283200E+00, 1.8937673438745800E-02, 9.4689017597904200E+00 },
        { 2.6750126053105600E+01, 2.5210843879897000E-01, 1.2605533317078200E+02 },
        { 1.1499966333358300E+01, -6.7333133247693400E-02, -3.3666491597195100E+01 },
        { -1.0499968999970900E+01, 6.2000232250447500E-02, 3.1000203226660900E+01 },
        { -1.7499867031521000E+01, 2.6593533177354700E-01, 1.3296685280828000E+02 },
        { -4.4249896591789300E+01, 2.0681646669018500E-01, 1.0340825604160300E+02 },
        { 1.0062574161761800E+01, 1.4832534480468900E-01, 7.4163582956643300E+01 },
        { 2.3187669349646100E+01, 3.3870341889574700E-01, 1.6935377278493000E+02 },
        { 1.0000070521111100E+00, 1.4104388927793600E-02, 7.0522778120585200E+00 },
        { 6.5000523752260100E+00, 1.0475180810720600E-01, 5.2376582095003900E+01 },
        { 4.4333613626038600E+01, 5.6059164256438900E-01, 2.8029893723768800E+02 },
        { 1.9375047656478200E+00, 9.5314325687656500E-03, 4.7657847461746100E+00 },
        { 1.7312610828532300E+01, 2.2165950894326100E-01, 1.1083097657582600E+02 },
        { -7.6666512500079500E+00, 3.0833269696994800E-02, 1.5416610988250300E+01 },
        { -5.4999779999943300E+00, 4.4000045334335800E-02, 2.2000039670011500E+01 },
        { 3.5000208209009000E+01, 4.1642207246184100E-01, 2.0821306340934500E+02 },
        { 2.2812384661626100E+01, -2.3067574097979100E-01, -1.1533736711299900E+02 },
        { -3.1562396901088700E+01, 2.0619754019968100E-01, 1.0309862894253200E+02 },
        { -2.5499708625944900E+01, 5.8274244078404500E-01, 2.9136838569954100E+02 },
        { -1.3099959046908900E+02, 8.1905978350960100E-01, 4.0952887312073400E+02 },
        { 1.1979264856565600E+01, 1.9638247313302700E-01, 9.8192574153443900E+01 },
        { 1.3000141719516500E+01, 2.8344363242993600E-01, 1.4172411588297900E+02 },
        { 5.0104572666538000E+01, 8.1201068938341300E-01, 4.0601081805746700E+02 },
        { 8.2500712086695800E+00, 1.4241935666614500E-01, 7.1210687084821600E+01 },
        { 3.6750307043085100E+01, 6.1409468141704300E-01, 3.0705159624488900E+02 },
        { 1.8750455275443900E-01, 9.1056292544078300E-03, 4.5528748152667800E+00 },
        { 5.0000514690012500E+00, 1.0293951002982000E-01, 5.1470508778628200E+01 },
        { 8.4563091512199000E+01, 1.1830389983913300E+00, 5.9152679936858000E+02 },
        { 4.4999956666867500E+00, -8.6665059964019500E-03, -4.3331927495060500E+00 },
        { 1.3500128333843900E+01, 2.5667075155588700E-01, 1.2833690761659900E+02 },
        { -1.9270762776768100E+01, 1.4111267769223400E-01, 7.0556112522268600E+01 },
        { -1.0999937031323300E+01, 1.2593691333417900E-01, 6.2968236673732700E+01 },
        { 2.1354453923457500E+01, 5.7451917119522000E-01, 2.8726238029008700E+02 },
        { 3.9749694375726700E+01, -6.1124418600400000E-01, -3.0561991277266900E+02 },
        { -6.6749674125452400E+01, 6.5174638085352300E-01, 3.2587183328303100E+02 },
        { -3.4999439783885200E+01, 1.1204164184910300E+00, 5.6020030371328700E+02 },
        { -2.9751945856788200E+02, 2.7495181438155200E+00, 1.3747526924419100E+03 } };

    /** Expected inclination function. */
    private final double[][] expected_F = {
        { 0.00000E+00, 0.00000E+00, 3.00000E+00, 0.00000E+00, 0.00000E+00, 3.00000E+00 },
        { 0.00000E+00, 0.00000E+00, -3.00000E+00, 0.00000E+00, 0.00000E+00, 3.00000E+00 },
        { 3.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { -1.50000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 1.50000E+01, 0.00000E+00, 0.00000E+00, 1.50000E+01 },
        { 0.00000E+00, 0.00000E+00, -1.50000E+01, 0.00000E+00, 0.00000E+00, 1.50000E+01 },
        { 1.50000E+01, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, -7.50000E+00, 0.00000E+00, 0.00000E+00, -7.50000E+00 },
        { 0.00000E+00, 0.00000E+00, 7.50000E+00, 0.00000E+00, 0.00000E+00, -7.50000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { -7.50000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 1.05000E+02, 0.00000E+00, 0.00000E+00, 1.05000E+02 },
        { 0.00000E+00, 0.00000E+00, -1.05000E+02, 0.00000E+00, 0.00000E+00, 1.05000E+02 },
        { 1.05000E+02, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 1.87500E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, -5.25000E+01, 0.00000E+00, 0.00000E+00, -5.25000E+01 },
        { 0.00000E+00, 0.00000E+00, 5.25000E+01, 0.00000E+00, 0.00000E+00, -5.25000E+01 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { -5.25000E+01, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 9.45000E+02, 0.00000E+00, 0.00000E+00, 9.45000E+02 },
        { 0.00000E+00, 0.00000E+00, -9.45000E+02, 0.00000E+00, 0.00000E+00, 9.45000E+02 },
        { 9.45000E+02, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 1.31250E+01, 0.00000E+00, 0.00000E+00, 1.31250E+01 },
        { 0.00000E+00, 0.00000E+00, -1.31250E+01, 0.00000E+00, 0.00000E+00, 1.31250E+01 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 1.31250E+01, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, -4.72500E+02, 0.00000E+00, 0.00000E+00, -4.72500E+02 },
        { 0.00000E+00, 0.00000E+00, 4.72500E+02, 0.00000E+00, 0.00000E+00, -4.72500E+02 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { -4.72500E+02, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 1.03950E+04, 0.00000E+00, 0.00000E+00, 1.03950E+04 },
        { 0.00000E+00, 0.00000E+00, -1.03950E+04, 0.00000E+00, 0.00000E+00, 1.03950E+04 },
        { 1.03950E+04, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { -2.18750E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 1.18125E+02, 0.00000E+00, 0.00000E+00, 1.18125E+02 },
        { 0.00000E+00, 0.00000E+00, -1.18125E+02, 0.00000E+00, 0.00000E+00, 1.18125E+02 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 1.18125E+02, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, -5.19750E+03, 0.00000E+00, 0.00000E+00, -5.19750E+03 },
        { 0.00000E+00, 0.00000E+00, 5.19750E+03, 0.00000E+00, 0.00000E+00, -5.19750E+03 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { -5.19750E+03, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 1.35135E+05, 0.00000E+00, 0.00000E+00, 1.35135E+05 },
        { 0.00000E+00, 0.00000E+00, -1.35135E+05, 0.00000E+00, 0.00000E+00, 1.35135E+05 },
        { 1.35135E+05, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 },
        { 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00, 0.00000E+00 } };

    /** Expected eccentricity function. */
    private final double[][] expected_G = { { 0, 0, 1, 0, 0, -1 }, { 0, 0, 1, 0, 0, 1 }, { 1, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 },
        { 0, 0, 1, 0, 0, -1 }, { 0, 0, 1, 0, 0, 1 }, { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 },
        { 0, 0, 1, 0, 0, -1 }, { 0, 0, 1, 0, 0, 1 }, { 0, 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0, -1 }, { 0, 0, 1, 0, 0, 1 }, { 1, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 },
        { 0, 0, 1, 0, 0, -1 }, { 0, 0, 1, 0, 0, 1 }, { 0, 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0, -1 }, { 0, 0, 1, 0, 0, 1 }, { 1, 0, 0, 0, 0, 0 },
        { 0, 0, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0, -1 }, { 0, 0, 1, 0, 0, 1 }, { 0, 0, 0, 0, 0, 0 },
        { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0, -1 }, { 0, 0, 1, 0, 0, 1 },
        { 0, 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0, -1 },
        { 0, 0, 1, 0, 0, 1 }, { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 },
        { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0, -1 }, { 0, 0, 1, 0, 0, 1 },
        { 0, 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0, -1 },
        { 0, 0, 1, 0, 0, 1 }, { 0, 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 },
        { 0, 0, 1, 0, 0, -1 }, { 0, 0, 1, 0, 0, 1 }, { 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } };

    /** Expected tesseral perturbation. */
    private final double[] expected_Tesseral = { 5.584714072275266212E-08, -5.305405061442123454E-02,
        -8.247514383227223887E-04,
        -4.477239732474820599E-02, 1.192930240201287129E+00, 2.540146534982363202E-01 };

    private StelaEquinoctialOrbit orbit;

    private StelaTesseralAttraction tesseralHarmonics;

    private StelaTesseralAttraction tesseralHarmonics2;

    private StelaTesseralAttraction tesseralHarmonics3;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Tesseral Perturbation
         * 
         * @featureDescription Validation of inner methods
         * 
         * @coveredRequirements
         */
        TESSERAL_INNER,

        /**
         * @featureTitle Tesseral Perturbation
         * 
         * @featureDescription Validation of perturbations
         * 
         * @coveredRequirements
         */
        TESSERAL_PERTURBATION,

        /**
         * @featureTitle Tesseral Perturbation
         * 
         * @featureDescription Validation of short periods
         * 
         * @coveredRequirements
         */
        TESSERAL_SHORT,

        /**
         * @featureTitle Tesseral Perturbation
         * 
         * @featureDescription Validation of partial derivatives
         * 
         * @coveredRequirements
         */
        TESSERAL_DV,

    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(StelaTesseralAttractionTest.class.getSimpleName(), "STELA tesseral attraction force");
    }

    @Before
    public void setup() throws IllegalArgumentException, PatriusException, IOException, ParseException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("regular-dataPBASE");
        EOPHistoryFactory.addEOP1980HistoryLoader(new EOP1980HistoryLoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name)
                                                                            throws IOException, ParseException,
                                                                            PatriusException {
            }

            @Override
            public void fillHistory(final EOP1980History history) throws PatriusException {
                double temp = 0;
                final double bias =
                    StelaTesseralAttractionTest.this.date.offsetFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH,
                        TimeScalesFactory.getUTC());
                for (int i = -30; i < 30; i++) {
                    temp = bias + i * 3 * 60 * 60;
                    history.addEntry(new EOP1980Entry((int) (temp / Constants.JULIAN_DAY), 0, 0, 0, 0, 0, 0));
                }
            }
        });
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final MyGRGSFormatReader reader = new MyGRGSFormatReader("stelaCoefficients", true);
        GravityFieldFactory.addPotentialCoefficientsReader(reader);
        this.date = new AbsoluteDate(new DateComponents(1997, 230), TimeComponents.H00, TimeScalesFactory.getUT1());
        this.date = new AbsoluteDate(new DateComponents(1997, 230), new TimeComponents(35), TimeScalesFactory.getTAI());

        this.orbit =
            new StelaEquinoctialOrbit(4.21642E7, 0, 0, 0, 0, 0, FramesFactory.getMOD(false), this.date,
                398600441449820.);
        this.tesseralHarmonics = new StelaTesseralAttraction(GravityFieldFactory.getPotentialProvider(), 7, 2,
            this.integrationStep,
            this.nbIntegrationStep);
        this.tesseralHarmonics.updateQuads(this.orbit);

        this.tesseralHarmonics2 = new StelaTesseralAttraction(GravityFieldFactory.getPotentialProvider());
        this.tesseralHarmonics2.updateQuads(this.orbit);

        this.tesseralHarmonics3 = new StelaTesseralAttraction(GravityFieldFactory.getPotentialProvider(), 256845, 2,
            this.integrationStep, 5);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TESSERAL_INNER}
     * 
     * @testedMethod {@link StelaTesseralAttraction#updateQuads(StelaEquinoctialOrbit, AbsoluteDate)}
     * 
     * @description tests the computation of quads
     * 
     * @input StelaEquinoctialOrbit and AbsoluteDate
     * 
     * @output Tesseral quads
     * 
     * @testPassCriteria references from Stela : 1e-14 relative scale for potential coefficients, central and delta
     *                   eccentricity
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testUpdateQuadsCase() throws PatriusException, IllegalArgumentException {
        final List<TesseralQuad> quadsList = this.tesseralHarmonics.getQuadsList();

        assertEquals(this.expected_quads.length, quadsList.size());
        for (int i = 0; i < quadsList.size(); i++) {
            checkInt(this.expected_quads[i], quadsList.get(i).getQuad(), 0);
            checkDouble(this.expected_fcfs1[i][0], quadsList.get(i).getFc(), tol_fcfs, ComparisonTypes.RELATIVE);
            checkDouble(this.expected_fcfs1[i][1], quadsList.get(i).getFs(), tol_fcfs, ComparisonTypes.RELATIVE);
            checkDouble(this.expected_eccentricity[i][0], quadsList.get(i).getCentralEccentricity(), tol_ec,
                ComparisonTypes.RELATIVE);
            checkDouble(this.expected_eccentricity[i][1], quadsList.get(i).getDeltaEccentricity(), tol_deltae,
                ComparisonTypes.RELATIVE);
            checkDouble(this.expected_Taylor[i], quadsList.get(i).getTaylorCoeffs(), tol_taylor,
                ComparisonTypes.RELATIVE);
            checkDouble(this.expected_TaylorDiff[i], quadsList.get(i).getDiffTaylorCoeffs(), tol_diffTaylor,
                ComparisonTypes.RELATIVE);

            // test updateEccentricityInterval

            final Orbit orb = new KeplerianOrbit(24500, 0.1, MathLib.toRadians(10), MathLib.toRadians(10),
                MathLib.toRadians(10), MathLib.toRadians(10), PositionAngle.MEAN, FramesFactory.getEME2000(),
                new AbsoluteDate(), 398600441449820.0);

            quadsList.get(0).updateEccentricityInterval(orb);
            Assert.assertEquals(0.1, quadsList.get(0).getCentralEccentricity(), 0);
        }
    }

    @Test
    public void coverageTest() throws IllegalArgumentException, PatriusException {

        // exception potential too high regarding the provider
        try {
            this.tesseralHarmonics3.updateQuads(this.orbit);
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        // test the memory process
        final int before = this.tesseralHarmonics2.getQuadsList().size();

        this.tesseralHarmonics2.computeCurrentQuad(this.orbit, 2, 2, 1, 0, -1, 5);
        final int after = this.tesseralHarmonics2.getQuadsList().size();

        Assert.assertEquals(before, after, 0);

        this.tesseralHarmonics2.computeCurrentQuad(this.orbit, 2, 2, 1, 0, -1, 0);
        final int after2 = this.tesseralHarmonics2.getQuadsList().size();

        Assert.assertEquals(before - 1, after2, 0);
    }

    /**
     * 
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TESSERAL_INNER}
     * 
     * @testedMethod {@link StelaTesseralAttraction#computeF}
     * 
     * @description tests the computation of inclination function f
     * 
     * @input StelaEquinoctialOrbit and AbsoluteDate
     * 
     * @output f
     * 
     * @testPassCriteria references from Stela : 1e-15 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputeInclinationFunctionCase()
                                                    throws PatriusException, IllegalArgumentException {
        final List<TesseralQuad> quadsList = this.tesseralHarmonics.getQuadsList();

        // Check result
        for (int i = 0; i < quadsList.size(); i++) {
            final double[] f = this.tesseralHarmonics.computeF(this.orbit, quadsList.get(i));
            checkDouble(this.expected_F[i], f, tol_f, ComparisonTypes.RELATIVE);
        }
    }

    /**
     * 
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TESSERAL_INNER}
     * 
     * @testedMethod {@link StelaTesseralAttraction#computeEccentricityFunction(StelaEquinoctialOrbit, TesseralQuad)}
     * 
     * @description tests the computation of eccentricity function
     * 
     * @input StelaEquinoctialOrbit and AbsoluteDate
     * 
     * @output f
     * 
     * @testPassCriteria references from Stela : 1e-15 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputeEccentricityFunctionCase() throws PatriusException, IllegalArgumentException {

        final List<TesseralQuad> quadsList = this.tesseralHarmonics.getQuadsList();

        // Check result
        for (int i = 0; i < quadsList.size(); i++) {
            final double[] ecc = this.tesseralHarmonics.computeEccentricityFunction(this.orbit,
                    quadsList.get(i));
            checkDouble(this.expected_G[i], ecc, tol_g, ComparisonTypes.RELATIVE);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TESSERAL_SHORT}
     * 
     * @testedMethod {@link StelaTesseralAttraction#computeShortPeriods(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of quads
     * 
     * @input StelaEquinoctialOrbit and AbsoluteDate
     * 
     * @output 0, not implemented
     * 
     * @testPassCriteria references from Stela : 1e-15 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputeShortPeriods() throws PatriusException {

        // Update quads
        final double[] result = this.tesseralHarmonics.computeShortPeriods(this.orbit);

        // Check result
        checkDouble(new double[6], result, tol_f, ComparisonTypes.ABSOLUTE);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#TESSERAL_DV}
     * 
     * @testedMethod {@link StelaTesseralAttraction#computeShortPeriods(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of quads
     * 
     * @input StelaEquinoctialOrbit and AbsoluteDate
     * 
     * @output 0, not implemented
     * 
     * @testPassCriteria references from Stela : 1e-15 relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputePartialDV() throws PatriusException {

        // Update quads
        final double[][] result = this.tesseralHarmonics.computePartialDerivatives(this.orbit);

        // Check result
        Assert.assertEquals(6, result.length);
        for (final double[] d : result) {
            checkDouble(new double[6], d, tol_f, ComparisonTypes.ABSOLUTE);
        }
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#TESSERAL_PERTURBATION}
     * 
     * @testedMethod {@link StelaTesseralAttraction#computePerturbation(StelaEquinoctialOrbit)}
     * 
     * @description tests the computation of quads
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output perturbation
     * 
     * @testPassCriteria references from Stela : 1e-13 relative scale
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testTesseralPerturbation()
                                          throws PatriusException, IOException, ParseException {

        Report.printMethodHeader("testTesseralPerturbation", "Perturbation computation", "STELA", 1E-13,
            ComparisonType.RELATIVE);

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17396 * 86400. + 35.);
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(new KeplerianOrbit(42164200, 0.004,
            MathLib.toRadians(45), MathLib.toRadians(7), MathLib.toRadians(12),
            MathLib.toRadians(200), PositionAngle.MEAN, FramesFactory.getCIRF(), date, Constants.CNES_STELA_MU));

        final StelaTesseralAttraction force = new StelaTesseralAttraction(GravityFieldFactory.getPotentialProvider());
        force.updateQuads(orbit);

        // Check result
        final double[] actual = force.computePerturbation(orbit);
        checkDouble(this.expected_Tesseral, actual, 1E-13, ComparisonTypes.RELATIVE);
        Report.printToReport("Perturbation", this.expected_Tesseral, actual);
    }

    public static void checkInt(final int iExpected, final int iActual, final int tol) {
        final int absDiff = iActual - iExpected;
        assertEquals(0, MathLib.abs(absDiff), tol);
    }

    public static void checkInt(final int[] dExpected, final int[] dActual, final int tol) {
        for (int i = 0; i < dExpected.length; i++) {
            checkInt(dExpected[i], dActual[i], tol);
        }
    }

    public static void checkDouble(final double dExpected, final double dActual, final double tol,
                                   final ComparisonTypes compType) {
        final double absDiff = dActual - dExpected;
        final double relDiff = dExpected == 0 & absDiff == 0 ? 0 : absDiff / dExpected;
        switch (compType) {
            case ABSOLUTE:
                assertEquals(0, MathLib.abs(absDiff), tol);
                break;
            case RELATIVE:
                assertEquals(0, MathLib.abs(relDiff), tol);
                break;
            default:
                break;
        }
    }

    public static void checkDouble(final double[] dExpected, final double[] dActual, final double[] tol,
                                   final ComparisonTypes compType) {
        for (int i = 0; i < dExpected.length; i++) {
            checkDouble(dExpected[i], dActual[i], tol[i], compType);
        }
    }

    public static void checkDouble(final double[] dExpected, final double[] dActual, final double tol,
                                   final ComparisonTypes compType) {
        for (int i = 0; i < dExpected.length; i++) {
            checkDouble(dExpected[i], dActual[i], tol, compType);
        }
    }

    /**
     * Enumeration of comparison possibilities.
     */
    public enum ComparisonTypes {
        ABSOLUTE, RELATIVE;
    }

}

// CHECKSTYLE:OFF
/*
 * NOTE
 * The GRGS reader, and abstract reader below extend the original classes because zonal and tesseral coefficients test
 * values are unnormalized, whereas the original implementations require normalized coefficients. The intent of theses
 * classes is to override the getC and getS method to return the coefficients as read, whitout any further operations.
 */

class MyGRGSFormatReader extends MyPotentialCoefficientsReader {

    /** Patterns for lines (the last pattern is repeated for all data lines). */
    private static final Pattern[] LINES;

    static {

        // sub-patterns
        final String real = "[-+]?\\d?\\.\\d+[eEdD][-+]\\d\\d";
        final String sep = ")\\s*(";

        // regular expression for header lines
        final String[] header = { "^\\s*FIELD - .*$", "^\\s+AE\\s+1/F\\s+GM\\s+OMEGA\\s*$",
            "^\\s*(" + real + sep + real + sep + real + sep + real + ")\\s*$",
            "^\\s*REFERENCE\\s+DATE\\s+:\\s+\\d.*$", "^\\s*MAXIMAL\\s+DEGREE\\s+:\\s+(\\d+)\\s.*$",
            // case insensitive for the next line
            "(?i)^\\s*L\\s+M\\s+DOT\\s+CBAR\\s+SBAR\\s+SIGMA C\\s+SIGMA S(\\s+LIB)?\\s*$" };

        // regular expression for data lines
        final String data = "^([ 0-9]{3})([ 0-9]{3})(   |DOT)\\s*(" + real + sep + real + sep + real + sep + real
            + ")(\\s+[0-9]+)?\\s*$";

        // compile the regular expressions
        LINES = new Pattern[header.length + 1];
        for (int i = 0; i < header.length; ++i) {
            LINES[i] = Pattern.compile(header[i]);
        }
        LINES[LINES.length - 1] = Pattern.compile(data);

    }

    /**
     * Simple constructor.
     * 
     * @param supportedNames
     *        regular expression for supported files names
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     */
    public MyGRGSFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                                                                    PatriusException {

        // FIELD - GRIM5, VERSION : C1, november 1999
        // AE 1/F GM OMEGA
        // 0.63781364600000E+070.29825765000000E+030.39860044150000E+150.72921150000000E-04
        // REFERENCE DATE : 1997.00
        // MAXIMAL DEGREE : 120 Sigmas calibration factor : .5000E+01 (applied)
        // L M DOT CBAR SBAR SIGMA C SIGMA S
        // 2 0DOT 0.13637590952454E-10 0.00000000000000E+00 .143968E-11 .000000E+00
        // 3 0DOT 0.28175700027753E-11 0.00000000000000E+00 .496704E-12 .000000E+00
        // 4 0DOT 0.12249148508277E-10 0.00000000000000E+00 .129977E-11 .000000E+00
        // 0 0 .99999999988600E+00 .00000000000000E+00 .153900E-09 .000000E+00
        // 2 0 -0.48416511550920E-03 0.00000000000000E+00 .204904E-10 .000000E+00

        final BufferedReader r = new BufferedReader(new InputStreamReader(input));
        boolean okConstants = false;
        boolean okMaxDegree = false;
        boolean okCoeffs = false;
        int lineNumber = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {

            ++lineNumber;

            // match current header or data line
            final Matcher matcher = LINES[MathLib.min(LINES.length, lineNumber) - 1].matcher(line);
            if (!matcher.matches()) {
                throw PatriusException.createParseException(PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber,
                    name, line);
            }

            if (lineNumber == 3) {
                // header line defining ae, 1/f, GM and Omega
                this.ae = Double.parseDouble(matcher.group(1).replace('D', 'E'));
                this.mu = Double.parseDouble(matcher.group(3).replace('D', 'E'));
                okConstants = true;
            } else if (lineNumber == 5) {
                // header line defining max degree
                final int maxDegree = Integer.parseInt(matcher.group(1));
                this.normalizedC = new double[maxDegree + 1][];
                this.normalizedS = new double[maxDegree + 1][];
                for (int k = 0; k < this.normalizedC.length; k++) {
                    this.normalizedC[k] = new double[k + 1];
                    this.normalizedS[k] = new double[k + 1];
                    if (!this.missingCoefficientsAllowed()) {
                        Arrays.fill(this.normalizedC[k], Double.NaN);
                        Arrays.fill(this.normalizedS[k], Double.NaN);
                    }
                }
                if (this.missingCoefficientsAllowed()) {
                    // set the default value for the only expected non-zero coefficient
                    this.normalizedC[0][0] = 1.0;
                }
                okMaxDegree = true;
            } else if (lineNumber > 6) {
                // data line
                if ("".equals(matcher.group(3).trim())) {
                    // non-dot data line
                    final int i = Integer.parseInt(matcher.group(1).trim());
                    final int j = Integer.parseInt(matcher.group(2).trim());
                    this.normalizedC[i][j] = Double.parseDouble(matcher.group(4).replace('D', 'E'));
                    this.normalizedS[i][j] = Double.parseDouble(matcher.group(5).replace('D', 'E'));
                    okCoeffs = true;
                }
            }

        }

        for (int k = 0; okCoeffs && k < this.normalizedC.length; k++) {
            final double[] cK = this.normalizedC[k];
            final double[] sK = this.normalizedS[k];
            for (int i = 0; okCoeffs && i < cK.length; ++i) {
                if (Double.isNaN(cK[i])) {
                    okCoeffs = false;
                }
            }
            for (int i = 0; okCoeffs && i < sK.length; ++i) {
                if (Double.isNaN(sK[i])) {
                    okCoeffs = false;
                }
            }
        }

        if (!(okConstants && okMaxDegree && okCoeffs)) {
            String loaderName = this.getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER, name, loaderName);
        }

        // normalizedC = normalize(normalizedC);
        // normalizedS = normalize(normalizedS);
        this.readCompleted = true;

    }
}

abstract class MyPotentialCoefficientsReader extends PotentialCoefficientsReader {

    /**
     * Constructor.
     */
    protected MyPotentialCoefficientsReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
    }

    @Override
    public double[][] getC(final int n, final int m, final boolean normalized) throws PatriusException {
        return this.truncateArray(n, m, super.getC(n, m, true));
    }

    @Override
    public double[][] getS(final int n, final int m, final boolean normalized) throws PatriusException {
        return this.truncateArray(n, m, super.getS(n, m, true));
    }

    private double[][] truncateArray(final int n, final int m, final double[][] complete) throws PatriusException {

        // safety checks
        if (n >= complete.length) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD, n, complete.length - 1);
        }
        if (m >= complete[complete.length - 1].length) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, m,
                complete[complete.length - 1].length - 1);
        }

        // truncate each array row in turn
        final double[][] result = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            final double[] ri = new double[MathLib.min(i, m) + 1];
            System.arraycopy(complete[i], 0, ri, 0, ri.length);
            result[i] = ri;
        }

        return result;

    }
}
