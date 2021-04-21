package one.tlph.blacksmith.eventbus;

import one.tlph.blacksmith.api.eventbus.EventPriority;
import one.tlph.blacksmith.api.eventbus.IEventListener;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public class ListenerList {
    private static List<ListenerList> allLists = new ArrayList<>();
    private static int maxSize = 0;

    @Nullable
    private ListenerList parent;
    private ListenerListInst[] lists = new ListenerListInst[0];

    public ListenerList()
    {
        this(null);
    }

    public ListenerList(@Nullable ListenerList parent)
    {
        // parent needs to be set before resize !
        this.parent = parent;
        extendMasterList(this);
        resizeLists(maxSize);
    }

    private synchronized static void extendMasterList(ListenerList inst)
    {
        allLists.add(inst);
    }

    static void resize(int max)
    {
        if (max > maxSize)
        {
            synchronized (ListenerList.class)
            {
                if (max > maxSize)
                {
                    allLists.forEach(list -> list.resizeLists(max));
                    maxSize = max;
                }
            }
        }
    }

    private synchronized void resizeLists(int max)
    {
        if (parent != null)
        {
            parent.resizeLists(max);
        }

        if (lists.length >= max)
        {
            return;
        }

        ListenerListInst[] newList = new ListenerListInst[max];
        int x = 0;
        for (; x < lists.length; x++)
        {
            newList[x] = lists[x];
        }
        for(; x < max; x++)
        {
            if (parent != null)
            {
                newList[x] = new ListenerListInst(parent.getInstance(x));
            }
            else
            {
                newList[x] = new ListenerListInst();
            }
        }
        lists = newList;
    }

    public static synchronized void clearBusID(int id)
    {
        for (ListenerList list : allLists)
        {
            list.lists[id].dispose();
        }
    }

    protected ListenerListInst getInstance(int id)
    {
        return lists[id];
    }

    public IEventListener[] getListeners(int id)
    {
        return lists[id].getListeners();
    }

    public void register(int id, EventPriority priority, IEventListener listener)
    {
        lists[id].register(priority, listener);
    }

    public void unregister(int id, IEventListener listener)
    {
        lists[id].unregister(listener);
    }

    public static synchronized void unregisterAll(int id, IEventListener listener)
    {
        for (ListenerList list : allLists)
        {
            list.unregister(id, listener);
        }
    }

    private class ListenerListInst
    {
        private boolean rebuild = true;
        private AtomicReference<IEventListener[]> listeners = new AtomicReference<>();
        private ArrayList<ArrayList<IEventListener>> priorities;
        private ListenerListInst parent;
        private List<ListenerListInst> children;
        private Semaphore writeLock = new Semaphore(1, true);


        private ListenerListInst()
        {
            int count = EventPriority.values().length;
            priorities = new ArrayList<>(count);

            for (int x = 0; x < count; x++)
            {
                priorities.add(new ArrayList<>());
            }
        }

        public void dispose()
        {
            writeLock.acquireUninterruptibly();
            priorities.forEach(ArrayList::clear);
            priorities.clear();
            writeLock.release();
            parent = null;
            listeners = null;
            if (children != null)
                children.clear();
        }

        private ListenerListInst(ListenerListInst parent)
        {
            this();
            this.parent = parent;
            this.parent.addChild(this);
        }

        /**
         * Returns a ArrayList containing all listeners for this event,
         * and all parent events for the specified priority.
         *
         * The list is returned with the listeners for the children events first.
         *
         * @param priority The Priority to get
         * @return ArrayList containing listeners
         */
        public ArrayList<IEventListener> getListeners(EventPriority priority)
        {
            writeLock.acquireUninterruptibly();
            ArrayList<IEventListener> ret = new ArrayList<>(priorities.get(priority.ordinal()));
            writeLock.release();
            if (parent != null)
            {
                ret.addAll(parent.getListeners(priority));
            }
            return ret;
        }

        /**
         * Returns a full list of all listeners for all priority levels.
         * Including all parent listeners.
         *
         * List is returned in proper priority order.
         *
         * Automatically rebuilds the internal Array cache if its information is out of date.
         *
         * @return Array containing listeners
         */
        public IEventListener[] getListeners()
        {
            if (shouldRebuild()) buildCache();
            return listeners.get();
        }

        protected boolean shouldRebuild()
        {
            return rebuild;// || (parent != null && parent.shouldRebuild());
        }

        protected void forceRebuild()
        {
            this.rebuild = true;
            if (this.children != null) {
                synchronized (this.children) {
                    for (ListenerListInst child : this.children)
                        child.forceRebuild();
                }
            }
        }

        private void addChild(ListenerListInst child)
        {
            if (this.children == null)
                this.children = Collections.synchronizedList(new ArrayList<>(2));
            this.children.add(child);
        }

        /**
         * Rebuild the local Array of listeners, returns early if there is no work to do.
         */
        private void buildCache()
        {
            if(parent != null && parent.shouldRebuild())
            {
                parent.buildCache();
            }
            ArrayList<IEventListener> ret = new ArrayList<>();
            Arrays.stream(EventPriority.values()).forEach(value -> {
                List<IEventListener> listeners = getListeners(value);
                if (listeners.size() > 0) {
                    ret.add(value); //Add the priority to notify the event of it's current phase.
                    ret.addAll(listeners);
                }
            });
            this.listeners.set(ret.toArray(new IEventListener[0]));
            rebuild = false;
        }

        public void register(EventPriority priority, IEventListener listener)
        {
            writeLock.acquireUninterruptibly();
            priorities.get(priority.ordinal()).add(listener);
            writeLock.release();
            this.forceRebuild();
        }

        public void unregister(IEventListener listener)
        {
            writeLock.acquireUninterruptibly();
            priorities.stream().filter(list -> list.remove(listener)).forEach(list -> this.forceRebuild());
            writeLock.release();
        }
    }
}