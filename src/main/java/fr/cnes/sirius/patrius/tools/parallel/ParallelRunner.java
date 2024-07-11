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
 * @history created 21/08/12
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.parallel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runner for parallel tests written as ParallelTask instances.
 * 
 * @useSample
 *            // Instantiate the tasks runner<br>
 *            final ParallelRunner runner = new ParallelRunner(5);<br>
 *            // Add task factories to the runner<br>
 *            runner.addTask(new TaskOneFactory(), 25);<br>
 *            runner.addTask(new TaskTwoFactory(), 15);<br>
 *            // Launch all tasks<br>
 *            final boolean result = runner.runAll();
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment instantiates a thread pool, would not be reasonable to add even more threads to the mix.
 * 
 * @author cardosop
 * 
 * @version $Id: ParallelRunner.java 17578 2017-05-10 12:20:20Z bignon $
 * 
 * @since 1.2
 * 
 */
public class ParallelRunner {

    /** Random generator. */
    private static final Random RANDOM = new Random();

    /** Default thread pool size. */
    private static final int DEFAULT_TPSIZE = 10;

    /** Thread pool size. */
    private int threadPoolSize;

    /** Thread pool. */
    private ExecutorService threadPool;

    /** Holders for the task factories. */
    private final List<TaskFactoryHolder> holders = new ArrayList<TaskFactoryHolder>();

    /** Map between factories and tasks. */
    private final Map<TaskFactoryHolder, List<ParallelTask>> taskMap =
        new HashMap<ParallelRunner.TaskFactoryHolder, List<ParallelTask>>();

    /** Human-readable result summary. */
    private String resultSummary = "";

    /** All tasks duration estimate, in milliseconds. */
    private long allTasksDurationEstimate;

    /** Effective duration in milliseconds. */
    private long effectiveDuration;

    /**
     * Default constructor.
     */
    public ParallelRunner() {
        this(DEFAULT_TPSIZE);
    }

    /**
     * Constructor with thread pool size parameter.
     * 
     * @param iThreadPoolSize
     *        thread pool size
     */
    public ParallelRunner(final int iThreadPoolSize) {
        // Thread pool initialisation
        this.resizeThreadPool(iThreadPoolSize);
    }

    /**
     * Adds a new task factory, with the number of instances
     * it should provide, OR updates the number of instances if
     * the task factory was added before.
     * The task factory provides ParallelTask instances.
     * Each instance will be run once and produce a result.
     * 
     * @param taskFactory
     *        the task factory
     * @param nbRuns
     *        the number of instances to create, equal to the number of "runs" for the matching ParallelTask class.
     */
    public void addTask(final ParallelTaskFactory<?> taskFactory, final int nbRuns) {
        if (taskFactory == null) {
            throw new ParallelException("null factory");
        }
        // Look for the task factory among the existing.
        boolean found = false;
        for (final TaskFactoryHolder holder : this.holders) {
            final ParallelTaskFactory<?> ptf = holder.getFactory();
            if (ptf.equals(taskFactory)) {
                found = true;
                // The task factory has already been set.
                // We only update the number of runs.
                holder.setNbRuns(Math.max(1, nbRuns));
                // Reset the factory (usually, the factory holds an id per task instance,
                // this call will reset them for the parallel runs.)
                ptf.reset();
            }
        }

        if (!found) {
            // The task factory was never added before.
            // Build the reference result
            final ParallelTask refTask = taskFactory.newInstance();
            final long startDt = (new Date()).getTime();
            final ParallelResult refRes = refTask.call();
            final long endDt = (new Date()).getTime();
            // Note the call to max to purge illegal int parameters
            final TaskFactoryHolder th =
                new TaskFactoryHolder(taskFactory, Math.max(1, nbRuns), refRes, endDt - startDt);
            this.holders.add(th);
            // Reset the factory (usually, the factory holds an id per task instance,
            // this call will reset them for the parallel runs.)
            taskFactory.reset();
        }
    }

    /**
     * Gets the result summary.
     * 
     * @return the result summary (empty string if called too early)
     */
    public String getResultSummary() {
        return this.resultSummary;
    }

    /**
     * Run all the tasks.
     * Only ends when all the tasks have run.
     * 
     * @return true if the results are as expected, false if some of the results differ from the reference.
     * @throws InterruptedException
     *         in case of thread pool interruption
     */
    public boolean runAll() throws InterruptedException {
        if (this.holders.size() == 0) {
            throw new ParallelException("nothing to run");
        }
        // Clears the task map, in case it was filled for a former run.
        this.taskMap.clear();
        // List of all tasks.
        // Will be submitted as a whole to the thread pool
        // when complete.
        final List<ParallelTask> allTasks = new ArrayList<ParallelTask>();

        // We will create a new task. It will be picked randomly
        // among the available factories, based on a weighted
        // probability according to the number of remaining runs
        int remainingRuns = 0;
        // Compute number of remaining runs
        for (final TaskFactoryHolder holder : this.holders) {
            remainingRuns += holder.getRemainingRuns();
        }
        while (remainingRuns > 0) {
            // Pick random slot
            final int randSlot = RANDOM.nextInt(remainingRuns);
            // Find the matching holder
            TaskFactoryHolder pickedHolder = null;
            int curPos = 0;
            for (final TaskFactoryHolder holder : this.holders) {
                final int hRemRuns = holder.getRemainingRuns();
                if (hRemRuns > 0) {
                    if (randSlot >= curPos && randSlot < (curPos + hRemRuns)) {
                        // The random slot is in the area of the
                        // current holder
                        pickedHolder = holder;
                        break;
                    } else {
                        // Go on
                        curPos += hRemRuns;
                    }
                }
            }
            // Add to list of all tasks
            final ParallelTaskFactory<?> factory = pickedHolder.getFactory();
            final ParallelTask newTask = factory.newInstance();
            allTasks.add(newTask);
            // Keep link to the new instance
            this.feedTaskMap(pickedHolder, newTask);
            // Update number of remaining runs :
            // Total number of runs ...
            remainingRuns--;
            // ... and number of runs for the picked factory
            pickedHolder.decreaseRemainingRuns();
        }

        // Feed the thread pool and wait.
        final boolean noTimeout = this.feedPoolAndWait(allTasks);

        boolean rez = true;
        if (!noTimeout) {
            // The parallel run took way too long
            // and timed out : this counts as failure.
            this.resultSummary = "The parallel run timed out.\n" + this.nominalSummary();
            rez = false;
        } else {
            // Process the results
            rez = this.processResults();
        }
        return rez;

    }

    /**
     * Resizes the thread pool.
     * 
     * @param thPoolSize
     *        new thread pool size
     */
    public void resizeThreadPool(final int thPoolSize) {
        if (thPoolSize < 1) {
            this.threadPoolSize = 1;
        } else {
            this.threadPoolSize = thPoolSize;
        }
        // Create new thread pool
        this.threadPool = Executors.newFixedThreadPool(this.threadPoolSize);
    }

    /**
     * Feeds the Map linking the factory holders and their tasks.
     * 
     * @param pickedHolder
     *        the factory holder
     * @param newTask
     *        the new task
     */
    private void feedTaskMap(final TaskFactoryHolder pickedHolder, final ParallelTask newTask) {

        // Check the taskMap for the holder
        if (!this.taskMap.containsKey(pickedHolder)) {
            // Create a new task list for this holder
            final List<ParallelTask> newList = new ArrayList<ParallelTask>();
            // Add to the map
            this.taskMap.put(pickedHolder, newList);
        }

        // Add the task to the list of tasks matching the holder
        this.taskMap.get(pickedHolder).add(newTask);
    }

    /**
     * Feeds the thread pool and waits for all tasks to complete.
     * 
     * @param allTasks
     *        list of all tasks to run.
     * @return true, or false if the timeout was reached before all tasks could complete.
     * @throws InterruptedException
     *         in case of thread pool interruption
     */
    private boolean feedPoolAndWait(final List<ParallelTask> allTasks) throws InterruptedException {

        // Estimate non-parallel duration
        // for all tasks computation
        this.estimateAllTasksDuration();

        // Compute a timeout duration.
        // The timeout is three times
        // the estimated non-parallel duration,
        // with a minimum of two seconds to account for
        // initialisation overhead in short tasks.
        final long timeoutDuration = Math.max(2000L, 3 * this.allTasksDurationEstimate);

        // Submit all the tasks to the thread pool at once.
        // Future results are not needed,
        // since we have a tasksMap.
        final long startPt = (new Date()).getTime();
        // Wait for the end of all tasks.
        final List<Future<ParallelResult>> futures = this.threadPool.invokeAll(allTasks, timeoutDuration,
            TimeUnit.MILLISECONDS);
        final long endPt = (new Date()).getTime();
        // Compute the effective duration
        this.effectiveDuration = endPt - startPt;
        // Determine if there was a timeout
        boolean rez = true;
        for (final Future<ParallelResult> fResult : futures) {
            if (fResult.isCancelled()) {
                // At least one task timed out
                rez = false;
                break;
            }
        }
        return rez;

    }

    /**
     * Estimate the time required to run all parallel tasks on a single thread, that is, on a non-parallel fashion.
     * 
     */
    private void estimateAllTasksDuration() {
        this.allTasksDurationEstimate = 0;
        for (final TaskFactoryHolder holder : this.holders) {
            final long hNbRuns = holder.getNbRuns();
            final long hRefDuration = holder.getReferenceDuration();
            this.allTasksDurationEstimate = this.allTasksDurationEstimate + (hRefDuration * hNbRuns);
        }
    }

    /**
     * Process the results.
     * 
     * @return true if the results are correct.
     */
    private boolean processResults() {
        // For each task factory holder, we recover the reference
        // result and compare the result of each task to
        // this reference.
        // All tasks need to have the same result as the reference.
        for (final Entry<TaskFactoryHolder, List<ParallelTask>> entry : this.taskMap.entrySet()) {
            final TaskFactoryHolder holder = entry.getKey();
            final ParallelResult refResult = holder.getReferenceResult();
            for (final ParallelTask task : this.taskMap.get(holder)) {
                if (!refResult.resultEquals(task.getResult())) {
                    // The results differ
                    this.resultSummary = "Results differ. Task : " + task.getTaskInfo();
                    return false;
                }
            }
        }
        this.resultSummary = this.nominalSummary();
        return true;
    }

    /**
     * Builds a nominal results summary.
     * 
     * @return a nominal results summary.
     */
    private String nominalSummary() {
        final double thousand = 1000.;
        final String lineOne =
            "Estimated non-parallel duration : " + (this.allTasksDurationEstimate / thousand) + " s.\n";
        final String lineTwo = "Effective duration : " + (this.effectiveDuration / thousand) + " s.";
        return lineOne + lineTwo;
    }

    /**
     * Inner holder for the task factories.
     * 
     * @concurrency not thread-safe
     * 
     * @concurrency.comment mutable instances
     * 
     * @author cardosop
     * 
     * @version $Id: ParallelRunner.java 17578 2017-05-10 12:20:20Z bignon $
     * 
     */
    private static final class TaskFactoryHolder {

        /** Number of runs. */
        private int nbRuns;

        /** Task factory. */
        private final ParallelTaskFactory<?> taskFactory;

        /** Reference result. */
        private final ParallelResult referenceResult;

        /** Reference duration in milliseconds. */
        private final long referenceDuration;

        /** Number of remaining runs. */
        private int remainingRuns;

        /**
         * Constructor.
         * 
         * @param iFactory
         *        the task factory
         * @param iNbRuns
         *        the number of runs = number of task instances
         * @param refResult
         *        the reference result
         * @param refDuration
         *        the reference duration
         */
        private TaskFactoryHolder(final ParallelTaskFactory<?> iFactory, final int iNbRuns,
            final ParallelResult refResult, final long refDuration) {
            this.taskFactory = iFactory;
            this.referenceResult = refResult;
            this.referenceDuration = refDuration;
            this.setNbRuns(iNbRuns);
        }

        /**
         * Getter for number of runs.
         * 
         * @return the number of runs.
         */
        private int getNbRuns() {
            return this.nbRuns;
        }

        /**
         * Setter for number of runs.
         * 
         * @param nbr
         *        the number of runs.
         */
        private void setNbRuns(final int nbr) {
            this.nbRuns = nbr;
            this.remainingRuns = this.nbRuns;
        }

        /**
         * Getter for number of remaining runs.
         * 
         * @return the number of remaining runs.
         */
        private int getRemainingRuns() {
            return this.remainingRuns;
        }

        /**
         * Decreases the number of remaining runs.
         */
        private void decreaseRemainingRuns() {
            this.remainingRuns--;
        }

        /**
         * Getter for the factory.
         * 
         * @return the task factory.
         */
        private ParallelTaskFactory<?> getFactory() {
            return this.taskFactory;
        }

        /**
         * Getter for the reference result.
         * 
         * @return the reference result.
         */
        private ParallelResult getReferenceResult() {
            return this.referenceResult;
        }

        /**
         * Getter for the reference duration.
         * 
         * @return the reference duration.
         */
        private long getReferenceDuration() {
            return this.referenceDuration;
        }
    }

}
