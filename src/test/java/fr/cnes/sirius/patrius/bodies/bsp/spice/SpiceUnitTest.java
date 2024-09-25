/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.Arrays;

import org.junit.Test;
import org.junit.Assert;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class has as its objective to do the unit tests to different basic class of the SPICE library translation
 * @author T0281925
 *
 */
public class SpiceUnitTest {

    @Test
    public void watcherUnitTest() throws PatriusException {
        Watcher w1 = new Watcher("test1");
        Watcher w3 = new Watcher("test1");
        Watcher w5 = new Watcher("test2");
        
        try {
            Watcher w2 = new Watcher("");
        } catch( IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        try {
            Watcher w4 = new Watcher(null);
        } catch( IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
          
        Assert.assertTrue(w1.hashCode() == w3.hashCode());
        Assert.assertTrue(w1.hashCode() != w5.hashCode());
        
        Assert.assertTrue(w1.equals(w1));
        Assert.assertFalse(w1.equals(null));
        Assert.assertTrue(w1.equals(w3));
        Assert.assertFalse(w1.equals(w5));
        Assert.assertFalse(w1.equals(new Integer(2)));
        
        
    }
    
    @Test
    public void spkSegmentUnitTest() {
        double [] desc = {5, 2, 3, 4, 1};
        String  id = "test";
        

        SpkSegment s1 = new SpkSegment(1,desc,id);
        SpkSegment s2 = new SpkSegment(2,desc,id);
        SpkSegment s3 = new SpkSegment(1,desc,id);
        SpkSegment s4 = new SpkSegment(2,desc,"");
        Arrays.sort(desc);
        SpkSegment s5 = new SpkSegment(2,desc,"");
        
        try {
            SpkSegment s6 = new SpkSegment(2,desc,null);
        } catch( IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        
        Assert.assertFalse(s1.hashCode() == s2.hashCode());
        
        Assert.assertFalse(s1.equals(s2));
        Assert.assertTrue(s1.equals(s1));
        Assert.assertFalse(s1.equals(null));
        Assert.assertFalse(s1.equals(id));
        Assert.assertTrue(s1.equals(s3));
        Assert.assertFalse(s1.equals(s4));
        Assert.assertFalse(s1.equals(s5));
        
    }
    
    @Test
    public void kernelPoolUnitTest() {
        String var1 = "var1";
        String var2 = "var2";
        
        String type1 = "numerical";
        String type2 = "character";
        
        KernelPool kp1 = new KernelPool(var1,type1);
        KernelPool kp2 = new KernelPool(var2,type2);
        KernelPool kp3 = new KernelPool(var1,type2);
        KernelPool kp4 = new KernelPool(var2,type1);
        KernelPool kp5 = new KernelPool(type1,var1);
        KernelPool kp8 = new KernelPool(var1,type1);
        
        try {
            KernelPool kp6 = new KernelPool(type1,null);
        } catch( IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        try {
            KernelPool kp7 = new KernelPool(null,null);
        } catch( IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        Assert.assertFalse(kp1.hashCode() == kp2.hashCode());
        Assert.assertFalse(kp1.hashCode() == kp5.hashCode());
        
        Assert.assertFalse(kp1.equals(kp2));
        Assert.assertFalse(kp1.equals(kp3));
        Assert.assertFalse(kp1.equals(kp4));
        Assert.assertFalse(kp1.equals(kp5));
        Assert.assertTrue(kp1.equals(kp1));
        Assert.assertTrue(kp1.equals(kp8));
        Assert.assertFalse(kp1.equals(null));
        Assert.assertFalse(kp1.equals(var1));
    }
    
     @Test
     public void inertialFramesUnitTest() {
         String name1 = "frm1";
         String name2 = "frm2";
         
         String base1 = "base1";
         String base2 = "base2";
         
         String defs1 = "def1";
         String defs2 = "def2";
         
         InertialFrames if1 = new InertialFrames(name1,base1,defs1);
         InertialFrames fif1 = new InertialFrames(name1);
         
         Assert.assertTrue(if1.hashCode() == fif1.hashCode());
         Assert.assertTrue(if1.equals(fif1));
         Assert.assertTrue(if1.equals(if1));
         Assert.assertFalse(if1.equals(null));
         Assert.assertFalse(if1.equals(name1));
         
         try {
             InertialFrames if2 = new InertialFrames(null,base1,defs1);
         } catch( IllegalArgumentException e) {
             Assert.assertTrue(true);
         }
         
         try {
             InertialFrames if3 = new InertialFrames(name1,null,defs1);
         } catch( IllegalArgumentException e) {
             Assert.assertTrue(true);
         }
         
         try {
             InertialFrames if4 = new InertialFrames(name1,base1,null);
         } catch( IllegalArgumentException e) {
             Assert.assertTrue(true);
         }
         
         try {
             InertialFrames fif2 = new InertialFrames(null);
         } catch( IllegalArgumentException e) {
             Assert.assertTrue(true);
         }
         
     }
     
     @Test 
     public void spiceKernelInfoUnitTest() {
         SpiceKernelInfo ski1 = new SpiceKernelInfo("file","type",1,1);
         SpiceKernelInfo ski2 = new SpiceKernelInfo("file2","type2",1,1);
         
         try {
             SpiceKernelInfo ski3 = new SpiceKernelInfo(null,"type",1,1);
         } catch( IllegalArgumentException e) {
             Assert.assertTrue(true);
         }
         
         try {
             SpiceKernelInfo ski4 = new SpiceKernelInfo("file",null,1,1);
         } catch( IllegalArgumentException e) {
             Assert.assertTrue(true);
         }
         
         Assert.assertFalse(ski1.hashCode() == ski2.hashCode());
         
         Assert.assertFalse(ski1.equals(null));
         Assert.assertFalse(ski1.equals(Integer.valueOf(1)));
         Assert.assertFalse(ski1.equals(ski2));
         Assert.assertTrue(ski1.equals(ski1));
         
     }
     
     @Test
     public void SpiceCommonTest() throws PatriusException {
         String test = "abcdefghijk";
         char ch = 'a';
         Assert.assertEquals(1, SpiceCommon.indexOfNoChar(test,ch));
         
         ch = 'c';
         Assert.assertEquals(0, SpiceCommon.indexOfNoChar(test,ch));
         
         test = "kkkkkkk";
         ch = 'k';
         Assert.assertEquals(-1, SpiceCommon.indexOfNoChar(test,ch));
         
         test = "kkkkkkkaaaaaaaa";
         ch = 'a';
         Assert.assertEquals(0, SpiceCommon.indexOfNoChar(test,ch));
         
         test = "kkkkkkkaaaaaaaa";
         ch = 'k';
         Assert.assertEquals(7, SpiceCommon.indexOfNoChar(test,ch));
         
         ch = '-';
         Assert.assertEquals(0, SpiceCommon.indexOfNoChar(test,ch));
         
         String[] unknown = SpiceCommon.idword2architype("");
         Assert.assertEquals("?",unknown[0]);
         Assert.assertEquals("?",unknown[1]);
         
         unknown = SpiceCommon.idword2architype("fakeIdWord");
         Assert.assertEquals("?",unknown[0]);
         Assert.assertEquals("?",unknown[1]);
     }
     
     @Test
     public void spkBodyUnitTest() {
         SpkBody sb1 = new SpkBody(1);
         SpkBody sb2 = new SpkBody(2);
         
         Assert.assertFalse(sb1.hashCode() == sb2.hashCode());
         
         Assert.assertTrue(sb1.getReuseExpense() == sb2.getReuseExpense());
         sb1.setReuseExpense(1);
         Assert.assertFalse(sb1.getReuseExpense() == sb2.getReuseExpense());
         
         Assert.assertTrue(sb1.equals(sb1));
         Assert.assertFalse(sb1.equals(sb2));
         Assert.assertFalse(sb1.equals(null));
         Assert.assertFalse(sb1.equals(Integer.valueOf(1)));
     }
     
     @Test
     public void spiceChangeFrameUnitTest() throws PatriusException {
         SpiceChangeFrame.frameRotationMatrix(3, 3);
         
         try {
             SpiceChangeFrame.frameRotationMatrix(3, 27);
         } catch(PatriusException e) {
             Assert.assertTrue(true);
         }
         
         try {
             SpiceChangeFrame.frameRotationMatrix(-2, 3);
         } catch(PatriusException e) {
             Assert.assertTrue(true);
         }
         
         try {
             SpiceChangeFrame.frameRotationMatrix(27, 3);
         } catch(PatriusException e) {
             Assert.assertTrue(true);
         }
         
         Assert.assertEquals(1, SpiceChangeFrame.intertialRefFrameNumber("J2000"));
         Assert.assertEquals(0, SpiceChangeFrame.intertialRefFrameNumber("DEFAULT"));
         
         
     }
     
     @Test
     public void dafStateUnitTest() {
         DafState ds1 = new DafState();
         DafState ds2 = new DafState();
         
         Assert.assertTrue(ds1.equals(ds2));
         
         ds1.setHandle(2);
         
         Assert.assertFalse(ds1.hashCode() == ds2.hashCode());
         
         Assert.assertFalse(ds1.equals(null));
         Assert.assertFalse(ds1.equals(Integer.valueOf(2)));
     }
     
     @Test
     public void dafReaderToolsUnitTest() {
         
         try {
             DafReaderTools.nRecord2nByte(0);
         } catch(MathIllegalArgumentException e) {
             Assert.assertTrue(true);
         }
         
         int[] record = new int[1];
         int[] word = new int[1];
         try {
             DafReaderTools.address2RecordWord(-1, record, word);
         } catch(MathIllegalArgumentException e) {
             Assert.assertTrue(true);
         }
     }
}
