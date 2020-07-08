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

import groovy.lang.Closure;
import groovy.transform.CompileStatic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.Validate.notNull;

@CompileStatic
public class CProf {
    public static final AtomicBoolean turnTreeProfilingOff = new AtomicBoolean(false);

    final static ThreadLocal<Deque<ProfileEvent>> profileEventStack = new ThreadLocal<Deque<ProfileEvent>>();
    final static ThreadLocal<ProfileEvent> rootEventThreadLocal = new ThreadLocal<ProfileEvent>();
    final static AtomicLong counter = new AtomicLong(0);
    final static ProfileEvent EMPTY = new ProfileEvent();
    static {
        EMPTY.setName("treeProfilingDisabled");
    }

    public static void clear(){
        rootEventThreadLocal.set(null);
        profileEventStack.set(null);
        counter.set(0);
    }

    public static void start(String name) {
        notNull(name);

        Deque<ProfileEvent> stack = profileEventStack.get();
        if (stack == null) {
            stack = new ArrayDeque<ProfileEvent>();
            profileEventStack.set(stack);
        }

        ProfileEvent event = null;
        ProfileEvent parent = getParent();
        if (parent != null && turnTreeProfilingOff.get()) {
            //if tree profiling is disabled, don't start any new ProfileEvents if there
            //is already a parent/root event
            return;
        }else if (parent != null) {
            event = parent.getEvent(name);
        }

        if (event == null) {
            event = new ProfileEvent();
            event.setName(name);
            if (parent != null) {
                parent.addChild(event);
            }
        }

        // if stack is empty then consider this to be an entry point
        if (stack.isEmpty()) {
            rootEventThreadLocal.set(event);
        }
        stack.push(event);
        event.startEvent();
    }

    public static ProfileEvent getParent() {
        ProfileEvent parent = null;
        Deque<ProfileEvent> stack = profileEventStack.get();
        if (stack != null) {
            parent = stack.peek();
        }
        return parent;
    }

    public static ProfileEvent stop(String name) {
        return stop(name, true);
    }

    public static ProfileEvent stop(String name, boolean aggregate) {
        notNull(name);
        ProfileEvent rootEvent = getRootEvent();
        boolean stoppingRoot = rootEvent != null && rootEvent.getName().equals(name);
        if(turnTreeProfilingOff.get() && !stoppingRoot){
            //if tree profiling is turned off and the call isn't to stop the rootEvent, return null

            //if disable gets set in between a start and stop call
            //we'll end up with invalid elements in the event stack, we need to clear those out
            Deque<ProfileEvent> stack = profileEventStack.get();
            while (stack.size() > 1) {
                //remove any ProfilingEvents that were started in between tree profiling being disabled and enabled
                ProfileEvent pe = stack.pop();
            }
            return EMPTY;
        }
        Deque<ProfileEvent> stack = profileEventStack.get();

        if(!stoppingRoot && stack.size() == 1){
            //tree profiling must have been re-enabled in between start and stop call
            //we can't stop this event as it was never started, return EMPTY results rather than throwing an exception.
            return EMPTY;
        }

        if (stack == null) {
            notNull(stack, "Must call start prior calling stop. Name [" + name + "]");
        }
        ProfileEvent event = stack.pop();
        notNull(event, "Must call start prior calling stop. Name=$name");
        if(!event.getName().equals(name)){
            throw new IllegalArgumentException("Current event's name=["+event.getName()+"] but stop name=["+name+"]");
        }
        event.endEvent();
        if (!aggregate) {
            String previousName = event.getName();
            event.setName(event.getName() + "_" + counter.getAndIncrement());
            if (event.getParent()!=null) {
                event.getParent().replaceChild(previousName, event);
            }
        }
        return event;
    }

    public static ProfileEvent prof(String name, Closure profileMe){
        return prof(name, true, profileMe);
    }

    public static ProfileEvent prof(String name, boolean aggregate, Closure profileMe) {
        if (!aggregate) {
            name = name + "_" + counter.getAndIncrement();
        }

        ProfileEvent res = null;
        start(name);
        boolean hadExcetpion = false;
        try {
            profileMe.call();
        } catch (Throwable t){
            hadExcetpion = true;
            throw t;
        } finally {
            try {
                res = stop(name);
            } catch (Throwable stopT){

                // the stack is officially broken since we couldn't stop a profiling event
                // the only thing to do is clear the stack so next profiling session is correct
                clear();

                // if stop method itself throws an exception it will hide the original exception
                if(!hadExcetpion) {
                    throw stopT;
                }
            }
        }
        return res;
    }

    public static String prettyPrint() {
        ProfileEvent profileEvent = getRootEvent();
        if(profileEvent!= null){
            return profileEvent.prettyPrint();
        }
        return "No profiling events";
    }

    public static ProfileEvent getRootEvent() {
        return rootEventThreadLocal.get();
    }

    public static void initRootEvent(ProfileEvent rootEvent) {
        ProfileEvent existing = getRootEvent();
        if(existing != null ){
            throw new IllegalArgumentException("Root event is already set. Event name is [" + existing.getName() + "]" );
        }
        rootEventThreadLocal.set(rootEvent);
        ArrayDeque stack = new ArrayDeque<ProfileEvent>();
        stack.push(rootEvent);
        profileEventStack.set(stack);
    }
}