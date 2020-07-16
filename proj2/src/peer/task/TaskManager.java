package peer.task;

import peer.chord.ChordNode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class TaskManager {

    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Task>> pendingTasks = new ConcurrentHashMap<>();

    public void addTask(int key, Task task) {
        ConcurrentLinkedQueue<Task> tasks = pendingTasks.computeIfAbsent(key, value -> new ConcurrentLinkedQueue<>());
        tasks.add(task);
    }
    
    public void completeTasks(int key, ChordNode chordNode) {
        ConcurrentLinkedQueue<Task> tasks = pendingTasks.get(key);

        if(tasks != null) {
            synchronized (tasks) {
                Task task;

                while ((task = tasks.poll()) != null) {
                    task.complete(chordNode);
                }
            }
        }
    }
}
