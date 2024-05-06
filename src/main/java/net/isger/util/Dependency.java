package net.isger.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Dependency {

    public static final String ROOT = "root";

    private volatile transient List<Object> stayings;

    private List<Object> stays;

    private List<Object> nodes;

    /** 正向依赖集合 */
    private Map<Object, List<Object>> dependencies;

    /** 反向依赖集合 */
    private Map<Object, List<Object>> bedependencies;

    public Dependency() {
        this.stayings = new LinkedList<Object>();
        this.nodes = new LinkedList<Object>();
        this.stays = new LinkedList<Object>();
        this.dependencies = new HashMap<Object, List<Object>>();
        this.bedependencies = new HashMap<Object, List<Object>>();
    }

    public List<Object> getNodes() {
        return Collections.unmodifiableList(this.nodes);
    }

    public void addNode(Object node, Object... dependencies) {
        this.addNode(node, Arrays.asList(dependencies));
    }

    public void addNode(Object node, List<Object> dependencies) {
        if (dependencies == null) dependencies = new ArrayList<Object>();
        else Asserts.throwArgument(!dependencies.contains(node), "Dependencies cannot contain itself [%s]", node);
        dependencies = Helpers.getMerge(this.dependencies.get(node), dependencies); // 合并依赖节点
        this.addDependencies(node, dependencies);
        this.setDependencies(node, dependencies);
    }

    /**
     * 添加依赖
     * 
     * @param node
     * @param dependencies
     */
    private void addDependencies(Object node, List<Object> dependencies) {
        this.dependencies.put(node, dependencies); // 替换依赖集合
        List<Object> bedependencies;
        for (Object dependency : dependencies) {
            bedependencies = this.bedependencies.get(dependency); // 反向依赖集合
            if (bedependencies == null) this.bedependencies.put(dependency, bedependencies = new ArrayList<Object>()); // 新增反向依赖
            else if (bedependencies.contains(node)) continue;
            bedependencies.add(node); // 添加反向依赖
        }
    }

    /**
     * 设置依赖
     * 
     * @param node
     * @param dependencies
     */
    private void setDependencies(Object node, List<Object> dependencies) {
        if (dependencies != null) {
            dependencies = Helpers.getSurplus(this.nodes, dependencies);
            if (dependencies.size() == 0) this.setNode(node);
            else {
                for (Object dependency : dependencies) this.addStay(dependency);
                this.addStay(node);
            }
        }
    }

    /**
     * 设置节点
     * 
     * @param node
     */
    private void setNode(Object node) {
        if (!this.nodes.contains(node)) this.nodes.add(node);
        List<Object> bedependencies = this.bedependencies.get(node);
        if (bedependencies != null) {
            for (Object bedependency : bedependencies) {
                this.setDependencies(bedependency, this.dependencies.get(bedependency));
            }
        }
        this.stays.remove(node);
    }

    /**
     * 逗留节点
     *
     * @param node
     */
    private void addStay(Object node) {
        Asserts.throwState(!stayings.contains(node), "Found the self-devourer: %s -> %s", this.stayings, node);
        this.stayings.add(node);
        this.nodes.remove(node);
        if (!this.stays.contains(node)) this.stays.add(node);
        List<Object> bedependencies = this.bedependencies.get(node);
        if (bedependencies != null) {
            for (Object bedependency : bedependencies) {
                this.addStay(bedependency);
            }
        }
        this.stayings.remove(node);
    }

}
