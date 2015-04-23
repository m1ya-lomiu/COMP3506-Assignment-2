package a2;

import java.util.*;

public class NaiveAllocator {

	/**
	 * @precondition: Neither of the inputs are null or contain null elements.
	 *                The parameter donations is a list of distinct donations
	 *                such that for each d in donations, d.getTotal() equals
	 *                d.getUnspent(); and for each p in projects
	 *                p.allocatedFunding() equals 0.
	 * @postcondition: returns false if there no way to completely fund all of
	 *                 the given projects using the donations, leaving both the
	 *                 input list of donations and set of projects unmodified;
	 *                 otherwise returns true and allocates to each project
	 *                 funding from the donations. The allocation to each
	 *                 project must be complete and may not violate the
	 *                 conditions of the donations.
	 */
	public static boolean canAllocate(List<Donation> donations,
			Set<Project> projects) {
		
		return canAllocateHelper(donations, projects, 0);
	}
	
	private static boolean canAllocateHelper(List<Donation> donations,
			Set<Project> projects, int i){
		
		// Are all the projects completely funded?
		boolean fullyFunded = true;
		
		for(Project project: projects) {
			if(!project.fullyFunded()) {
				fullyFunded = false;
			}
		}
		
		// Yes! All the projects are completely funded
		if(fullyFunded == true) {
			return true;
		}
		
		// No more donations to allocate
		if(i == donations.size()) {
			return false;
		}
		
		Donation thisDonation = donations.get(i);
		
		// No funds left in this donation
		if(thisDonation.spent() == true) {
			return canAllocateHelper(donations, projects, i+1);
		}
		
		// No projects that this donation can spend on that still need funding
		Set<Project> avaliableProjects = thisDonation.getProjects();
		boolean needFund = false;
		for(Project testNeedFundP: avaliableProjects) {
			if(testNeedFundP.fullyFunded() == false) {
				// This project still needs funding!!!
				needFund = true;
			}
		}
		
		if(needFund == false) {
			return canAllocateHelper(donations, projects, i+1);
		}
				
		for(Project p: avaliableProjects) {
			if(p.fullyFunded() == false) {
				p.allocate(thisDonation, 1);
				if(canAllocateHelper(donations, projects, i)) {
					return true;
				} else{
					p.deallocate(thisDonation, 1);
				}
			}
		}
		
		return false;
	}

}
