/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.caching.internal.tasks;

import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.changedetection.state.CurrentTaskExecution;
import org.gradle.api.internal.changedetection.state.ValueSnapshot;
import org.gradle.caching.internal.DefaultBuildCacheHasher;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.hash.HashCode;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

public class TaskCacheKeyCalculator {

    private final boolean buildCacheDebugLogging;

    public TaskCacheKeyCalculator(boolean buildCacheDebugLogging) {
        this.buildCacheDebugLogging = buildCacheDebugLogging;
    }

    public TaskOutputCachingBuildCacheKey calculate(TaskInternal task, CurrentTaskExecution execution) {
        TaskOutputCachingBuildCacheKeyBuilder builder = new DefaultTaskOutputCachingBuildCacheKeyBuilder(task.getIdentityPath());
        if (buildCacheDebugLogging) {
            builder = new DebuggingTaskOutputCachingBuildCacheKeyBuilder(builder);
        }
        builder.appendTaskImplementation(execution.getTaskImplementation());
        builder.appendTaskActionImplementations(execution.getTaskActionImplementations());

        SortedMap<String, ValueSnapshot> inputProperties = execution.getInputProperties();
        for (Map.Entry<String, ValueSnapshot> entry : inputProperties.entrySet()) {
            DefaultBuildCacheHasher newHasher = new DefaultBuildCacheHasher();
            entry.getValue().appendToHasher(newHasher);
            if (newHasher.isValid()) {
                HashCode hash = newHasher.hash();
                builder.appendInputValuePropertyHash(entry.getKey(), hash);
            } else {
                builder.inputPropertyImplementationUnknown(entry.getKey());
            }
        }

        SortedMap<String, CurrentFileCollectionFingerprint> inputFingerprints = execution.getInputFingerprints();
        for (Map.Entry<String, CurrentFileCollectionFingerprint> entry : inputFingerprints.entrySet()) {
            builder.appendInputFilesProperty(entry.getKey(), entry.getValue());
        }

        SortedSet<String> outputPropertyNamesForCacheKey = execution.getOutputPropertyNamesForCacheKey();
        for (String cacheableOutputPropertyName : outputPropertyNamesForCacheKey) {
            builder.appendOutputPropertyName(cacheableOutputPropertyName);
        }

        return builder.build();
    }
}
