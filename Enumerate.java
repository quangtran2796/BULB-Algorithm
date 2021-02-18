package scheduler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Enumerate extends PartialScheduler{
	private Schedule best_schedule;
	private int best_latency;
	private Map<Integer, Set<String>> res_used;
	private Map<String, Set<RT>> res;
	
	public Enumerate(Schedule alap) {
		best_schedule = new Schedule();
		res_used = new TreeMap<Integer, Set<String>>();
		res = new HashMap<String, Set<RT>>();
		best_schedule = alap;
		best_latency = Integer.MAX_VALUE;
	}
	@Override
	public Schedule schedule(Graph sg, RC rc, Schedule sched, Integer curr_node_number, Schedule asap, Schedule alap){
		
		Schedule schedule = sched.clone();
		//resources_used_list(schedule);
		res = rc.getAllRes();
		
		if(curr_node_number == sg.size()+1) {
			//Ask prof. here why i = N + 1, but schedule only N
			//PartialScheduler upper_bound_schedule = new Upper();
			//Schedule s = upper_bound_schedule.schedule(sg, rc, schedule, curr_node_number);
			if(best_latency > schedule.max()) {
				best_schedule = schedule;
			}
		}
		else {
			
			Node curr_nd = sg.get_node(curr_node_number);
			int asap_temp = asap.slot(curr_nd).lbound;
			int alap_temp = alap.slot(curr_nd).lbound;
			for(int step = asap.slot(curr_nd).lbound; step <= alap.slot(curr_nd).lbound; step++) {
				Schedule saved_asap = asap.clone();  
				if(resources(step, curr_nd.getRT()).size() < rc.getRes(curr_nd.getRT()).size()) {
					
					PartialScheduler lower_bound_estimate = new Lower();
					Schedule s_lower = lower_bound_estimate.schedule(sg, rc,  sched,  curr_node_number - 1, asap, alap);
					Integer l = s_lower.max() + 1;
					s_lower.draw("schedules/s_lower_.dot");
					
					PartialScheduler upper_bound_estimate = new Upper();
					Schedule s_upper = upper_bound_estimate.schedule(sg, rc,  sched,  curr_node_number - 1, asap, alap);
					Integer u = s_upper.max() + 1;
					s_upper.draw("schedules/s_upper_.dot");
					
					if(u < best_latency) {
						best_schedule = s_upper.clone();
						best_schedule.draw("schedules/best_schedule_.dot");
						best_latency = u;
					}
					
					if(l < best_latency) {
						Interval ii = new Interval(step, step + curr_nd.getDelay() - 1);
						for(String res : rc.getRes(curr_nd.getRT())) {
							if(!resources(step, curr_nd.getRT()).contains(res)) {
								
								schedule.add(curr_nd, ii, res);
								
								//increase ResourceUsed()
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
								break;
							}
						}
						
						//ASAP update should be written here and call enumerate again.
						updateASAP(curr_nd, schedule.slot(curr_nd).lbound, asap,saved_asap);
						int as_temp = asap.max();
						//Scheduler s_new = new ALAP(asap.max() + 1);
						//Schedule alap_new = s_new.schedule(sg);
						schedule(sg, rc, schedule, curr_node_number+1, asap, alap);
						
						//Decrement ResourceUsed()
						for(int k = schedule.slot(curr_nd).lbound.intValue(); k <= schedule.slot(curr_nd).ubound.intValue(); k++) 
						{
							res_used.get(k).remove(schedule.resource(curr_nd));
						}
						schedule.remove(curr_nd);
					}
				}
				
				asap = saved_asap;
			}
		}
		
		
		return best_schedule;
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
	
	private void resources_used_list(Schedule schedule) {
		
		for (Node nd : schedule.nodes()) {
			Interval ii = schedule.slot(nd);
			for(int step = ii.lbound.intValue(); step <= ii.ubound.intValue(); step++) {
				if(res_used.containsKey(step))
					//Get resource corresponding to nd and add to the list 
					res_used.get(step).add(schedule.resource(nd));
				else {
					Set<String> hash_set = new HashSet<String>();
					hash_set.add(schedule.resource(nd));
					res_used.put(step, hash_set);
				}
					
			}
		}
	}
	
	private void updateASAP(Node curr_node, Integer step, Schedule asap, Schedule asap_ref) {
		if(curr_node.leaf()) {
			
		}
		else {
			for(Node nd : curr_node.successors()) {
				int lowerbound = Math.max(asap.slot(nd).lbound, step + asap_ref.slot(nd).lbound - asap_ref.slot(curr_node).lbound);
				Interval ii = new Interval(lowerbound, lowerbound+nd.getDelay()-1);
				asap.remove(nd);
				asap.add(nd, ii);
				updateASAP(nd, lowerbound, asap, asap_ref);
			}
		}
	}
}
