/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package callStack.profiler;

import org.apache.commons.lang3.Validate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notNull;

public class ProfileEvent implements Serializable {
    static final long serialVersionUID = 1l;

    long runtimeInMillis = 0;
    int numOfInvocations = 0;
    String name;

    // interal use only please
    long start = -1;
    // these are display only
    boolean isConcurrent = false;
    boolean isRemote = false;
    ProfileEvent parent;
    // ---------------------------------
    Map<String, ProfileEvent> childrenAsMap;

    public Collection<ProfileEvent> getChildren() {
        if (childrenAsMap == null) {
            return Collections.emptyList();
        }
        return childrenAsMap.values();
    }

    public synchronized void addChild(ProfileEvent child) {
        notNull(child);
        notNull(child.getName());

        if (childrenAsMap == null) {
            childrenAsMap = new ConcurrentHashMap<String, ProfileEvent>();
        }
        childrenAsMap.put(child.getName(), child);
        child.setParent(this);
    }

    synchronized void replaceChild(String previousName, ProfileEvent child) {
        childrenAsMap.remove(previousName);
        childrenAsMap.put(child.getName(), child);
    }


    public ProfileEvent getEvent(String str) {
        notNull(str);
        ProfileEvent res = null;
        if (childrenAsMap != null) {
            res = childrenAsMap.get(str);
        }
        return res;
    }

    public void startEvent() {
        if (start != -1) {
            Validate.isTrue(start == -1, "Can not start event twice. Event [" + name + "] has already been started");
        }

        start = System.currentTimeMillis();
    }

    public void endEvent() {
        if (start == -1) {
            throw new IllegalArgumentException("Must call startEvent first");
        }
        numOfInvocations++;
        runtimeInMillis = runtimeInMillis + (System.currentTimeMillis() - start);
        start = -1;
    }

    public String prettyPrint() {
        StringBuilder res = new StringBuilder();
        buildPrettyString(res, this, "");
        return res.toString();
    }

    private final static NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

    private void buildPrettyString(StringBuilder res, ProfileEvent node, String pre) {
        if (res.length() > 0) {
            res.append("\n");
        }
        StringBuilder preBuilder = new StringBuilder(pre);
        if (node.isConcurrent) {
            preBuilder.append("|");
        }
        if (node.isRemote) {
            preBuilder.append("||");
        }
        preBuilder.append("|");
        res.append(preBuilder.toString());

        res.append("-> ");
        res.append(node.getName());
        res.append(" (");
        res.append(NUMBER_FORMAT.format(node.getNumOfInvocations()));
        res.append(") : ");
        addRuntime(res, node.getRuntimeInMillis());

        boolean hasChildren = node != null && !isEmpty(node.getChildrenAsMap());
        if (hasChildren) {
            handleUnaccountedTime(res, node);
        }
        if (hasChildren) {
            preBuilder.append("     ");
            for (ProfileEvent profileEvent : node.getChildrenAsMap().values()) {
                buildPrettyString(res, profileEvent, preBuilder.toString());
            }
        }
    }

    private static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }

    private void handleUnaccountedTime(StringBuilder res, ProfileEvent node) {
        Collection<ProfileEvent> values = node.getChildrenAsMap().values();

        long childrenSum = 0;
        List<ProfileEvent> syncEvents = values.stream().filter(p-> !isConcurrent(p)).collect(Collectors.toList());
        if(!syncEvents.isEmpty()){
            childrenSum += syncEvents.stream().mapToLong(ProfileEvent::getRuntimeInMillis).sum();
        }
        List<ProfileEvent> asyncEvents = values.stream().filter(p-> isConcurrent(p)).collect(Collectors.toList());
        if(!asyncEvents.isEmpty()){
            childrenSum += asyncEvents.stream().mapToLong(ProfileEvent::getRuntimeInMillis).max().getAsLong();
        }

        long diff = node.getRuntimeInMillis() - childrenSum;
        res.append(" [");
        res.append(periodFormatter.print(new Period(diff)));
        res.append("]");
    }
    private boolean isConcurrent(ProfileEvent p){
        return p.isRemote() || p.isConcurrent();
    }

    private final static long SECOND = 1000;
    private final static long MINUTE = 60 * SECOND;
    private final static long HOUR = 60 * MINUTE;


    private static final PeriodFormatter periodFormatter = new PeriodFormatterBuilder()
            .appendHours()
            .appendSuffix("h")
            .appendSeparatorIfFieldsBefore(" ")
            .appendMinutes()
            .appendSuffix("m")
            .appendSeparatorIfFieldsBefore(" ")
            .appendSeconds()
            .appendSuffix("s")
            .appendSeparatorIfFieldsBefore(" ")
            .appendMillis3Digit()
            .appendSuffix("ms").toFormatter();

    private void addRuntime(StringBuilder res, long runtime) {
        res.append(periodFormatter.print(new Period(runtime)));
    }

    public boolean isEnded() {
        return start == -1;
    }

    @Override
    public String toString() {
        return prettyPrint();
    }

    public long getRuntimeInMillis() {
        return runtimeInMillis;
    }

    public void setRuntimeInMillis(long runtimeInMillis) {
        this.runtimeInMillis = runtimeInMillis;
    }

    public int getNumOfInvocations() {
        return numOfInvocations;
    }

    public void setNumOfInvocations(int numOfInvocations) {
        this.numOfInvocations = numOfInvocations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public boolean isConcurrent() {
        return isConcurrent;
    }

    public void setConcurrent(boolean concurrent) {
        isConcurrent = concurrent;
    }

    public boolean isRemote() {
        return isRemote;
    }

    public void setRemote(boolean remote) {
        isRemote = remote;
    }

    public ProfileEvent getParent() {
        return parent;
    }

    public void setParent(ProfileEvent parent) {
        this.parent = parent;
    }

    public Map<String, ProfileEvent> getChildrenAsMap() {
        return childrenAsMap;
    }

    public void setChildrenAsMap(Map<String, ProfileEvent> childrenAsMap) {
        this.childrenAsMap = childrenAsMap;
    }
}

