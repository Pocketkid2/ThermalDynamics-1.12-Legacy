package cofh.thermaldynamics.duct.energy;

import cofh.redstoneflux.impl.EnergyStorage;
import cofh.thermaldynamics.multiblock.IGridTile;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import cofh.thermaldynamics.multiblock.MultiBlockGridTracking;
import net.minecraft.world.World;

public class GridEnergy extends MultiBlockGridTracking<DuctUnitEnergy> {

	public static int NODE_STORAGE[] = { 1000 * 5, 4000 * 5, 9000 * 5, 16000 * 5, 25000 * 5, 0 };
	public static int NODE_TRANSFER[] = { 1000, 4000, 9000, 16000, 25000, 0 };
	public final EnergyStorage myStorage;
	private final int transferLimit;

	private final int capacity;
	private int currentEnergy = 0;
	private int extraEnergy = 0;

	public GridEnergy(World world, int transferLimit, int capacity) {

		super(world);

		this.transferLimit = transferLimit;
		this.capacity = capacity;
		myStorage = new EnergyStorage(GridEnergy.this.capacity, GridEnergy.this.transferLimit) {

			@Override
			public int receiveEnergy(int maxReceive, boolean simulate) {

				return trackIn(super.receiveEnergy(maxReceive, simulate), simulate);
			}

			@Override
			public int extractEnergy(int maxExtract, boolean simulate) {

				return trackOut(super.extractEnergy(maxExtract, simulate), simulate);
			}
		};
	}

	@Override
	public void addNode(DuctUnitEnergy aMultiBlock) {

		super.addNode(aMultiBlock);

		if (aMultiBlock.getEnergyForGrid() > 0) {
			myStorage.modifyEnergyStored(aMultiBlock.getEnergyForGrid());
		}
	}

	@Override
	public void balanceGrid() {

		myStorage.setCapacity(nodeSet.size() * capacity);
	}

	@Override
	public void destroyNode(IGridTile node) {

		if (node.isNode()) {
			((DuctUnitEnergy) node).setEnergyForGrid(getNodeShare((DuctUnitEnergy) node));
		}
		super.destroyNode(node);
	}

	@Override
	public void removeBlock(DuctUnitEnergy oldBlock) {

		if (oldBlock.isNode()) {
			oldBlock.setEnergyForGrid(getNodeShare(oldBlock));
		}
		super.removeBlock(oldBlock);
	}

	@Override
	public void tickGrid() {

		super.tickGrid();

		if (!nodeSet.isEmpty() && myStorage.getEnergyStored() > 0) {
			currentEnergy = myStorage.getEnergyStored() / nodeSet.size();
			extraEnergy = myStorage.getEnergyStored() % nodeSet.size();
			for (IGridTile m : nodeSet) {
				if (!m.tickPass(0) || m.getGrid() == null) {
					break;
				}
			}
		}
	}

	@Override
	public boolean canAddBlock(IGridTile aBlock) {

		return aBlock instanceof DuctUnitEnergy && ((DuctUnitEnergy) aBlock).getTransferLimit() == transferLimit;
	}

	@Override
	public boolean canGridsMerge(MultiBlockGrid grid) {

		return super.canGridsMerge(grid) && ((GridEnergy) grid).transferLimit == this.transferLimit;
	}

	@Override
	protected int getLevel() {

		return myStorage.getEnergyStored();
	}

	@Override
	protected String getUnit() {

		return "RF";
	}

	/* HELPERS */
	public void useEnergy(int energyUsed) {

		myStorage.extractEnergy(energyUsed, false);

		if (energyUsed > currentEnergy) {
			extraEnergy -= (energyUsed - currentEnergy);
			extraEnergy = Math.max(0, extraEnergy);
		}
	}

	public boolean isPowered() {

		return myStorage.getEnergyStored() > 0;
	}

	public int getNodeShare(DuctUnitEnergy ductEnergy) {

		return nodeSet.size() == 1 ? myStorage.getEnergyStored() : isFirstMultiblock(ductEnergy) ? myStorage.getEnergyStored() / nodeSet.size() + myStorage.getEnergyStored() % nodeSet.size() : myStorage.getEnergyStored() / nodeSet.size();
	}

	public int getSendableEnergy() {

		return Math.min(myStorage.getMaxExtract(), currentEnergy == 0 ? extraEnergy : currentEnergy);
	}

	public int receiveEnergy(int maxReceive, boolean simulate) {

		return myStorage.receiveEnergy(maxReceive, simulate);
	}

}
