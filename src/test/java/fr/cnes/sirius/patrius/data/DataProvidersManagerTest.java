/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:274:24/10/2014:third body ephemeris clearing modified
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.data;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class DataProvidersManagerTest {

    @Test
    public void testDefaultConfiguration() throws PatriusException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, this.getPath("regular-data"));
        final CountingLoader crawler = new CountingLoader(false);
        DataProvidersManager.getInstance().clearProviders();
        Assert.assertFalse(DataProvidersManager.getInstance().isSupported(
            new DirectoryCrawler(new File(this.getPath("regular-data")))));
        Assert.assertTrue(DataProvidersManager.getInstance().feed(".*", crawler));
        Assert.assertEquals(19, crawler.getCount());
    }

    @Test
    public void testLoadMonitoring() throws PatriusException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, this.getPath("regular-data"));
        final DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.clearProviders();
        manager.clearLoadedDataNames();
        Assert.assertFalse(manager.isSupported(new DirectoryCrawler(new File(this.getPath("regular-data")))));
        Assert.assertEquals(0, manager.getLoadedDataNames().size());
        final CountingLoader tleCounter = new CountingLoader(false);
        Assert.assertTrue(manager.feed(".*\\.tle$", tleCounter));
        Assert.assertEquals(3, tleCounter.getCount());
        Assert.assertEquals(3, manager.getLoadedDataNames().size());
        final CountingLoader de405Counter = new CountingLoader(false);
        Assert.assertTrue(manager.feed(".*\\.405$", de405Counter));
        Assert.assertEquals(4, de405Counter.getCount());
        Assert.assertEquals(7, manager.getLoadedDataNames().size());
        manager.clearLoadedDataNames();
        Assert.assertEquals(0, manager.getLoadedDataNames().size());
    }

    @Test
    public void testLoadFailure() {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, this.getPath("regular-data"));
        DataProvidersManager.getInstance().clearProviders();
        final CountingLoader crawler = new CountingLoader(true);
        try {
            DataProvidersManager.getInstance().feed(".*", crawler);
            Assert.fail("an exception should have been thrown");
        } catch (final PatriusException oe) {
            // expected
        }
        Assert.assertEquals(19, crawler.getCount());
    }

    @Test
    public void testEmptyProperty() throws PatriusException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, "");
        final CountingLoader crawler = new CountingLoader(false);
        DataProvidersManager.getInstance().clearProviders();
        DataProvidersManager.getInstance().feed(".*", crawler);
        Assert.assertEquals(0, crawler.getCount());
    }

    @Test(expected = PatriusException.class)
    public void testInexistentDirectory() throws PatriusException {
        final File inexistent = new File(this.getPath("regular-data"), "inexistent");
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, inexistent.getAbsolutePath());
        final CountingLoader crawler = new CountingLoader(false);
        DataProvidersManager.getInstance().clearProviders();
        DataProvidersManager.getInstance().feed(".*", crawler);
    }

    @Test(expected = PatriusException.class)
    public void testInexistentZipArchive() throws PatriusException {
        final File inexistent = new File(this.getPath("regular-data"), "inexistent.zip");
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, inexistent.getAbsolutePath());
        final CountingLoader crawler = new CountingLoader(false);
        DataProvidersManager.getInstance().clearProviders();
        DataProvidersManager.getInstance().feed(".*", crawler);
    }

    @Test(expected = PatriusException.class)
    public void testNeitherDirectoryNorZip() throws PatriusException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, this.getPath("regular-data/UTC-TAI.history"));
        final CountingLoader crawler = new CountingLoader(false);
        DataProvidersManager.getInstance().clearProviders();
        DataProvidersManager.getInstance().feed(".*", crawler);
    }

    @Test
    public void testListModification() throws PatriusException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, this.getPath("regular-data"));
        final CountingLoader crawler = new CountingLoader(false);
        final DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.clearProviders();
        Assert.assertFalse(manager.isSupported(new DirectoryCrawler(new File(this.getPath("regular-data")))));
        Assert.assertTrue(manager.feed(".*", crawler));
        Assert.assertTrue(crawler.getCount() > 0);
        final List<DataProvider> providers = manager.getProviders();
        Assert.assertEquals(1, providers.size());
        for (final DataProvider provider : providers) {
            Assert.assertTrue(manager.isSupported(provider));
        }
        Assert.assertNotNull(manager.removeProvider(providers.get(0)));
        Assert.assertEquals(0, manager.getProviders().size());
        final DataProvider provider = new DataProvider(){
            @Override
            public boolean feed(final Pattern supported, final DataLoader visitor) throws PatriusException {
                return true;
            }
        };
        manager.addProvider(provider);
        Assert.assertEquals(1, manager.getProviders().size());
        manager.addProvider(provider);
        Assert.assertEquals(2, manager.getProviders().size());
        Assert.assertNotNull(manager.removeProvider(provider));
        Assert.assertEquals(1, manager.getProviders().size());
        Assert.assertNull(manager.removeProvider(new DataProvider(){
            @Override
            public boolean feed(final Pattern supported, final DataLoader visitor) throws PatriusException {
                throw new PatriusException(new DummyLocalizable("oops!"));
            }
        }));
        Assert.assertEquals(1, manager.getProviders().size());
        Assert.assertNotNull(manager.removeProvider(manager.getProviders().get(0)));
        Assert.assertEquals(0, manager.getProviders().size());
    }

    @Test
    public void testComplexPropertySetting() throws PatriusException {
        final String sep = System.getProperty("path.separator");
        final File top = new File(this.getPath("regular-data"));
        final File dir1 = new File(top, "de405-ephemerides");
        final File dir2 = new File(new File(top, "Earth-orientation-parameters"), "monthly");
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH,
            dir1 + sep + sep + sep + sep + dir2);
        DataProvidersManager.getInstance().clearProviders();

        CountingLoader crawler = new CountingLoader(false);
        Assert.assertTrue(DataProvidersManager.getInstance().feed(".*\\.405$", crawler));
        Assert.assertEquals(4, crawler.getCount());

        crawler = new CountingLoader(false);
        Assert.assertTrue(DataProvidersManager.getInstance().feed(".*\\.txt$", crawler));
        Assert.assertEquals(1, crawler.getCount());

        crawler = new CountingLoader(false);
        Assert.assertTrue(DataProvidersManager.getInstance().feed("bulletinb_.*\\.txt$", crawler));
        Assert.assertEquals(2, crawler.getCount());

    }

    @Test
    public void testMultiZip() throws PatriusException {
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, this.getPath("zipped-data/multizip.zip"));
        final CountingLoader crawler = new CountingLoader(false);
        DataProvidersManager.getInstance().clearProviders();
        Assert.assertTrue(DataProvidersManager.getInstance().feed(".*\\.txt$", crawler));
        Assert.assertEquals(6, crawler.getCount());
    }

    private static class CountingLoader implements DataLoader {
        private final boolean shouldFail;
        private int count;

        public CountingLoader(final boolean shouldFail) {
            this.shouldFail = shouldFail;
            this.count = 0;
        }

        @Override
        public boolean stillAcceptsData() {
            return true;
        }

        @Override
        public void loadData(final InputStream input, final String name)
                                                                        throws PatriusException {
            ++this.count;
            if (this.shouldFail) {
                throw new PatriusException(new DummyLocalizable("intentional failure"));
            }
        }

        public int getCount() {
            return this.count;
        }
    }

    private String getPath(final String resourceName) {
        try {
            final ClassLoader loader = DirectoryCrawlerTest.class.getClassLoader();
            return loader.getResource(resourceName).toURI().getPath();
        } catch (final URISyntaxException e) {
            Assert.fail(e.getLocalizedMessage());
            return null;
        }
    }

}
