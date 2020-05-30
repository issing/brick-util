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
        stayings = new LinkedList<Object>();
        nodes = new LinkedList<Object>();
        stays = new LinkedList<Object>();
        dependencies = new HashMap<Object, List<Object>>();
        bedependencies = new HashMap<Object, List<Object>>();
    }

    public List<Object> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public void addNode(Object node, Object... dependencies) {
        addNode(node, Arrays.asList(dependencies));
    }

    public void addNode(Object node, List<Object> dependencies) {
        if (dependencies == null) {
            dependencies = new ArrayList<Object>();
        } else {
            Asserts.throwArgument(!dependencies.contains(node), "Dependencies cannot contain itself [%s]", node);
        }
        dependencies = Helpers.getMerge(this.dependencies.get(node), dependencies); // 合并依赖节点
        addDependencies(node, dependencies);
        setDependencies(node, dependencies);
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
            if (bedependencies == null) {
                this.bedependencies.put(dependency, bedependencies = new ArrayList<Object>()); // 新增反向依赖
            } else if (bedependencies.contains(node)) {
                continue;
            }
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
            if (dependencies.size() == 0) {
                this.setNode(node);
            } else {
                for (Object dependency : dependencies) {
                    this.addStay(dependency);
                }
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
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
        List<Object> bedependencies = this.bedependencies.get(node);
        if (bedependencies != null) {
            for (Object bedependency : bedependencies) {
                setDependencies(bedependency, this.dependencies.get(bedependency));
            }
        }
        stays.remove(node);
    }

    /**
     * 逗留节点
     *
     * @param node
     */
    private void addStay(Object node) {
        Asserts.throwState(!stayings.contains(node), "Found the self-devourer: %s -> %s", stayings, node);
        stayings.add(node);
        nodes.remove(node);
        if (!stays.contains(node)) {
            stays.add(node);
        }
        List<Object> bedependencies = this.bedependencies.get(node);
        if (bedependencies != null) {
            for (Object bedependency : bedependencies) {
                addStay(bedependency);
            }
        }
        stayings.remove(node);
    }

}
