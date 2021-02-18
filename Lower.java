package scheduler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Lower extends PartialScheduler {
	private Map<Integer, Set<String>> res_used;
	private Map<String, Set<RT>> res;
	@Override
	public Schedule schedule(final Graph sg, final RC rc, final Schedule sched, final Integer curr_node_number, final Schedule asap, final Schedule alap) { 		

		res = rc.getAllRes();
		
		res_used = new TreeMap<Integer, Set<String>>();	//<timestep, Resource used>

		Schedule schedule = new Schedule();
		schedule = sched.clone();
		 
		
		//Here could have the error of comparing int and Integer
		//This loop will ceate a map of used resources at each time steps in schedule sched
		//Resources <- ResourceUsed
		if(curr_node_number != 0) {
			for (Node nd : schedule.nodes()) {
				Interval ii = schedule.slot(nd);
				for(int k = ii.lbound.intValue(); k <= ii.ubound.intValue(); k++) {
					if(res_used.containsKey(k))
						//Get resource corresponding to nd and add to the list 
						res_used.get(k).add(schedule.resource(nd));
					else {
						Set<String> hash_set = new HashSet<String>();
						hash_set.add(schedule.resource(nd));
						res_used.put(k, hash_set);
					}
						
				}
				
			}
		}
		
		//latency_estimate
		Integer latency_estimate = 0;
		if(curr_node_number != 0) {
			for(Node nd : schedule.nodes()) {
				for(Node nsd : nd.successors())	//successor of nd
				{
					Integer cp_succ = schedule.slot(nd).ubound + sg.critical_path(nsd);
					if(cp_succ > latency_estimate)
						latency_estimate = cp_succ; 
				}
			}
		}
		
		
		//Here should check the size is from 0 or 1 
		for(int i = curr_node_number + 1; i <= sg.size(); i++) {
			
			Node curr_nd = sg.get_node(i);	//get node from the graph from a Treemap
			// The priority are sorted when run numbering 
			
			Integer curr_lbound = asap.slot(curr_nd).lbound;	//k<--ASAP(xi)
			
			//Now we will check if the resource for this node is available at this
			//time step. If not -> move to the next time step.
			Set<String> max_resources = new HashSet<String>();
			max_resources = rc.getRes(curr_nd.getRT());
			
			//Loop from lbound to ubound and check if the required resource is available for the whole interval
			while(resources(curr_lbound, curr_nd.getRT()).size() >= max_resources.size()) {		//if == , all resources are used
				curr_lbound = curr_lbound + 1;
			}
			//After the previous loop, we have the start time step for curr_nd
			//Now assign resource to operator -> Here the simple method is implemented 
			//Later can improve by considering the weight between resources
			
			Set<String> curr_res_busy = resources(curr_lbound, curr_nd.getRT());
			for(String res : max_resources) {
				if(!curr_res_busy.contains(res)) {
					Interval ii = new Interval(curr_lbound,curr_lbound + curr_nd.getDelay()-1);
					schedule.add(curr_nd, ii , res);
					
					/*for(Node nd : curr_nd.predecessors())
						nd.handle(curr_nd);
					
					for(Node nd : curr_nd.successors())
						nd.handle(curr_nd);
						*/
					
					for(int k = ii.lbound.intValue(); k <= ii.ubound.intValue(); k++) {
						if(res_used.containsKey(k))
							//Get resource corresponding to nd and add to the list 
							res_used.get(k).add(schedule.resource(curr_nd));
						else {
							Set<String> hash_set = new HashSet<String>();
							hash_set.add(schedule.resource(curr_nd));
							res_used.put(k, hash_set);
						}
					}
					for (Node successor: curr_nd.successors()) //question: if there are more than two successors, which value shoud I return?
					{
						latency_estimate = Math.max(latency_estimate, curr_lbound + sg.critical_path(successor));
					}
					break;	//don't need to find another available resources
				}
			}
			//latency estimate need to be finshed!!!!!!!!
		
		}
		return schedule;
		
	}
	//This function return the used resources at lbound to the end of current op
	private Set<String> resources(Integer lbound, RT op){
		Set<String> set_res_used = new HashSet<String>();
		Set<String> temp_set = new HashSet<String>();
		//Get all resources are used in current step
		for(int k = lbound; k <= lbound + op.delay -1; k++) {
			if(res_used.containsKey(k)) {
				temp_set = res_used.get(k);
				if(!temp_set.isEmpty()) {
					for(String step_res : temp_set) {
						//If resource doesn't provide the considered op, it is removed from the set 
						if(res.get(step_res).contains(op)) {
							set_res_used.add(step_res);
						}
					}
				}
			}
		}
		return set_res_used;
	}
}
