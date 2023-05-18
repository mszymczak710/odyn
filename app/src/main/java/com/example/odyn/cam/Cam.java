package com.example.odyn.cam;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.example.odyn.FileHandler;
import com.example.odyn.main_service.types.IconType;

import java.io.File;

// przykrywa CamAccess (inherit or field?)
public class Cam extends CamAccess {

	private boolean isRecording = false; // nagranie zapisywane (blokowanie usuwania)
	private boolean isEmergency = false; // nagranie awaryjne zapisywane (blokowanie usuwania)
	public Cam(Activity mainActivity) {
		super(mainActivity);
		Log.v("Cam", ">>> Class Cam is ready");
	}

	// w CamAccess publiczne tylko:
	// takePicture(File file)
	// takeVideo(File file, boolean opcja)
	//
	// tutaj zamiast ich:
	// photo()
	// record()
	// emergency()
	// uruchamiane poprzez camAction(IconType)

	// obsłuż intent'y. żądania nagrywania, itp.
	public void onHandleIntent(Intent intent) { // ???
		// odczytaj, co zrobić RecType i ActionType
		if(intent != null) {
			if(intent.hasExtra("RecType") && intent.hasExtra("ActionType")) {
				Log.w("Cam", ">>> odbieranie Intent'ów z polem \"RecType\" przez Cam nie jest już wspierane");
			}
			if(intent.hasExtra("IconType")) {
				IconType iconType = (IconType) intent.getSerializableExtra("IconType");
				camAction(iconType);
			}
		}
	}


	public void camAction(IconType iconType) {
		switch(iconType) {
			case photo:
				photo(1);
				break;
			case recording:
				record();
				break;
			case emergency:
				emergency();
				break;
			default:
				// nic nie rób, są inne typy ikon, których Cam nie obsłuży
				break;
		}
	}
	private void photo(int opcja) {
			File file = new FileHandler(main).createPicture();
			takePicture(file);
	}
	private void record() {
		if (!isRecording) {
			Log.v("Cam", ">>> rozpoczynam nagrywanie");
			isRecording = true;
			takeVideo(isRecording);
		} else {
			Log.v("Cam", ">>> kończę nagrywanie");
			isRecording = false;
			takeVideo(isRecording);
		}
	}
	// może jeszcze zostać zmieniony format, albo dodane jakieś dane jeszcze
	private void emergency() {
		//File file = new FileHandler(main).createEmergencyVideo("mp4");
		if (!isEmergency) {
			isEmergency = true;
			takeVideo(isEmergency);
		} else {
			isEmergency = false;
			takeVideo(isEmergency);
		}
	}

	// śmieć:
	@Deprecated
	private void camAction(RecType rt, ActionType at) {
		switch(rt) {
			case picture:
				break;
			// notTODO
		}
	}
}
