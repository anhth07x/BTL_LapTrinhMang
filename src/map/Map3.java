/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package map;

import consts.Consts;
import java.io.Serializable;
import model.Brick;

/**
 *
 * @author tienanh
 */
public class Map3 extends Map implements Serializable{
	public Map3() {
		super();
		int padding = 10;
		int row = 16;
		int col = 21;
		mapState.setRow(row);
		mapState.setCol(col);
		mapState.setBricks(new Brick[row * col]);
		for (int i = 0 ; i < row ; i++) {
			for (int j = 0; j < col; j++){
				Brick tmp = new Brick(Consts.BRICK_WIDTH ,Consts.BRICK_HEIGHT, 70 + j * Consts.BRICK_WIDTH + j*padding, 200+ i*Consts.BRICK_HEIGHT + i*padding);
				mapState.getBricks()[i*col + j] = tmp;
				if ( (j + i) % 2 == 0 ) mapState.getBricks()[i*col + j].setIsDisplay(false);
			}	
		}
		
		// Map Info
        mapInfo.setName("Map3");
		mapInfo.setType("Small");
		mapInfo.setDes("More Info Here: github.com/tienanhnguyen191999/BTL_LapTrinhMang");
		mapInfo.setImagePreviewPath("/data/mapPreview/map-3.png");
	}
	
	
}
