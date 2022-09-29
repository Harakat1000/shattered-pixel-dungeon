/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2022 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChallenges;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHeroInfo;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndKeyBindings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.GameMath;
import com.watabou.utils.PointF;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HeroSelectScene extends PixelScene {

	private Image background;
	private Image fadeLeft, fadeRight;
	private IconButton btnFade; //only on landscape

	//fading UI elements
	private RenderedTextBlock title;
	private ArrayList<StyledButton> heroBtns = new ArrayList<>();
	private RenderedTextBlock heroName; //only on landscape
	private RenderedTextBlock heroDesc; //only on landscape
	private StyledButton startBtn;
	private IconButton infoButton;
	private IconButton btnOptions;
	private GameOptions optionsPane;
	private IconButton btnExit;

	@Override
	public void create() {
		super.create();

		Dungeon.hero = null;

		Badges.loadGlobal();
		Journal.loadGlobal();

		background = new Image(HeroClass.WARRIOR.splashArt()){
			@Override
			public void update() {
				if (rm > 1f){
					rm -= Game.elapsed;
					gm = bm = rm;
				} else {
					rm = gm = bm = 1;
				}
			}
		};
		background.scale.set(Camera.main.height/background.height);

		background.x = (Camera.main.width - background.width())/2f;
		background.y = (Camera.main.height - background.height())/2f;
		if (landscape()){
			//FIXME static offset and UI width falls apart at high resolutions
			//Should try not always having the UI stuck to the left, perhaps occupy the left 20% or so instead
			//Should also really try to have more variable spacing between UI elements so it fills things in a bit more
			background.x += 40;
		}
		background.visible = false;
		PixelScene.align(background);
		add(background);

		fadeLeft = new Image(TextureCache.createGradient(0xFF000000, 0xFF000000, 0x00000000));
		fadeLeft.x = background.x-2;
		fadeLeft.scale.set(3, background.height());
		add(fadeLeft);

		fadeRight = new Image(fadeLeft);
		fadeRight.x = background.x + background.width() + 2;
		fadeRight.y = background.y + background.height();
		fadeRight.angle = 180;
		add(fadeRight);

		if (landscape()){
			float fadeLeftScale = 47 * (80 - background.x)/80f;
			fadeLeft.scale = new PointF(3 + Math.max(0, fadeLeftScale), background.height());
		}

		startBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, ""){
			@Override
			protected void onClick() {
				super.onClick();

				if (GamesInProgress.selectedClass == null) return;

				Dungeon.hero = null;
				Dungeon.daily = Dungeon.dailyReplay = false;
				ActionIndicator.action = null;
				InterlevelScene.mode = InterlevelScene.Mode.DESCEND;

				Game.switchScene( InterlevelScene.class );
			}
		};
		startBtn.icon(Icons.get(Icons.ENTER));
		startBtn.setSize(80, 21);
		startBtn.setPos((Camera.main.width - startBtn.width())/2f, (Camera.main.height - HeroBtn.HEIGHT + 2 - startBtn.height()));
		add(startBtn);
		startBtn.visible = false;

		infoButton = new IconButton(Icons.get(Icons.INFO)){
			@Override
			protected void onClick() {
				super.onClick();
				Window w = new WndHeroInfo(GamesInProgress.selectedClass);
				if (landscape()){
					w.offset(40, 0);
				}
				ShatteredPixelDungeon.scene().addToFront(w);
			}

			@Override
			protected String hoverText() {
				return Messages.titleCase(Messages.get(WndKeyBindings.class, "hero_info"));
			}
		};
		infoButton.visible = false;
		infoButton.setSize(20, 21);
		add(infoButton);

		for (HeroClass cl : HeroClass.values()){
			HeroBtn button = new HeroBtn(cl);
			add(button);
			heroBtns.add(button);
		}

		int btnWidth = HeroBtn.MIN_WIDTH;
		float curX;
		if (landscape()){
			btnWidth = HeroBtn.MIN_WIDTH + 15;
			int cols = (1 + heroBtns.size())/2;
			curX = (Camera.main.width - btnWidth * cols + (cols-1))/2f;
		} else {
			curX = (Camera.main.width - btnWidth * heroBtns.size())/2f;
			if (curX > 0) {
				btnWidth += Math.min(curX / (heroBtns.size() / 2f), 15);
				curX = (Camera.main.width - btnWidth * heroBtns.size()) / 2f;
			}
		}

		float curY;
		if (landscape()){
			curY = (Camera.main.height-HeroBtn.HEIGHT)/2f + 5;
		} else {
			curY = Camera.main.height-HeroBtn.HEIGHT+3;
		}

		if (landscape()){
			int count = 0;
			for (StyledButton button : heroBtns){
				button.setRect(curX, curY, btnWidth, HeroBtn.HEIGHT);
				align(button);
				curX += btnWidth+1;
				count++;
				if (count >= (1+heroBtns.size())/2){
					curX -= btnWidth*count + count;
					curY += HeroBtn.HEIGHT+1;
					if (heroBtns.size()%2 != 0){
						curX += btnWidth/2f;
					}
					count = 0;
				}
			}
		} else {
			for (StyledButton button : heroBtns){
				button.setRect(curX, curY, btnWidth, HeroBtn.HEIGHT);
				curX += btnWidth;
			}
		}

		if (landscape()){
			heroName = renderTextBlock(9);
			add(heroName);

			heroDesc = renderTextBlock(5);
			heroDesc.align(RenderedTextBlock.CENTER_ALIGN);
			add(heroDesc);

			btnFade = new IconButton(Icons.COMPASS.get()){
				@Override
				protected void onClick() {
					uiAlpha = 0;
					updateFade();
				}
			};
			btnFade.icon().originToCenter();
			btnFade.icon().angle = 270f;
			btnFade.visible = btnFade.active = false;
			add(btnFade);
		}

		optionsPane = new GameOptions();
		optionsPane.visible = optionsPane.active = false;
		optionsPane.layout();
		optionsPane.setPos(heroBtns.get(0).left(), 0);
		add(optionsPane);

		btnOptions = new IconButton(Icons.get(Icons.PREFS)){
			@Override
			protected void onClick() {
				super.onClick();
				optionsPane.visible = !optionsPane.visible;
				optionsPane.active = !optionsPane.active;
			}

			@Override
			protected void onPointerDown() {
				super.onPointerDown();
			}

			@Override
			protected void onPointerUp() {
				updateOptionsColor();
			}

			@Override
			protected String hoverText() {
				return Messages.get(HeroSelectScene.class, "options");
			}
		};
		btnOptions.setRect(heroBtns.get(0).left() + 16, Camera.main.height-HeroBtn.HEIGHT-16, 20, 21);
		updateOptionsColor();
		btnOptions.visible = false;

		if (DeviceCompat.isDebug() || Badges.isUnlocked(Badges.Badge.VICTORY)){
			add(btnOptions);
		} else {
			Dungeon.challenges = 0;
			SPDSettings.challenges(0);
			SPDSettings.customSeed("");
		}

		btnExit = new ExitButton();
		btnExit.setPos( Camera.main.width - btnExit.width(), 0 );
		add( btnExit );
		btnExit.visible = btnExit.active = !SPDSettings.intro();

		title = PixelScene.renderTextBlock(Messages.get(this, "title"), 12);
		title.hardlight(Window.TITLE_COLOR);
		if (landscape()){
			title.setPos( (Camera.main.width - title.width())/2f, (Camera.main.height - title.height())/2f - 20);
		} else {
			title.setPos( (Camera.main.width - title.width())/2f, (Camera.main.height - HeroBtn.HEIGHT - title.height() - 4));
		}
		PixelScene.align(title);
		add(title);

		PointerArea fadeResetter = new PointerArea(0, 0, Camera.main.width, Camera.main.height){
			@Override
			public boolean onSignal(PointerEvent event) {
				if (event != null && event.type == PointerEvent.Type.UP) resetFade();
				return false;
			}
		};
		add(fadeResetter);
		resetFade();

		if (GamesInProgress.selectedClass != null){
			setSelectedHero(GamesInProgress.selectedClass);
		}

		fadeIn();

	}

	private void updateOptionsColor(){
		if (!SPDSettings.customSeed().isEmpty()){
			btnOptions.icon().hardlight(1f, 1.5f, 0.67f);
		} else if (SPDSettings.challenges() != 0){
			btnOptions.icon().hardlight(2f, 1.33f, 0.5f);
		} else {
			btnOptions.icon().resetColor();
		}
	}

	private void setSelectedHero(HeroClass cl){
		GamesInProgress.selectedClass = cl;

		background.texture( cl.splashArt() );
		background.visible = true;
		background.hardlight(1.5f,1.5f,1.5f);

		if (landscape()) {

			float dX = title.left();
			float dY = title.top();
			title.setPos((100 - title.width())/2f, (Camera.main.height - 140)/2f);
			align(title);

			dX = title.left() - dX;
			dY = title.top() - dY;

			if (dX != 0 || dY != 0) {
				for (StyledButton button : heroBtns) {
					button.setPos(button.left()+dX, button.top()+dY);
				}
			}

			startBtn.text(Messages.titleCase(Messages.get(HeroSelectScene.class, "start")));

		} else {
			title.visible = false;
			startBtn.text(Messages.titleCase(cl.title()));
		}

		if (landscape()){
			heroName.text(Messages.titleCase(cl.title()));
			heroName.hardlight(Window.TITLE_COLOR);
			heroName.setPos((100 - heroName.width() - 20)/2f, heroBtns.get(heroBtns.size()-1).bottom()+5);
			align(heroName);

			heroDesc.text(cl.shortDesc());
			heroDesc.maxWidth(80);
			heroDesc.setPos(10, heroName.bottom()+5);
			align(heroDesc);

			//TODO don't like the positioning of this
			btnFade.setRect(0, title.top()-10, 100, 10);
			btnFade.visible = btnFade.active = true;
		}

		startBtn.visible = true;
		startBtn.textColor(Window.TITLE_COLOR);
		startBtn.setSize(startBtn.reqWidth() + 8, 21);
		if (landscape()) {
			startBtn.setPos((100 - startBtn.width())/2f, heroName.bottom()+40);
		} else {
			startBtn.setPos((Camera.main.width - startBtn.width())/2f, startBtn.top());
		}
		PixelScene.align(startBtn);

		if (landscape()){

			infoButton.visible = true;
			infoButton.setPos(heroName.right(), heroName.top() + (heroName.height() - infoButton.height())/2f);
			align(infoButton);

			btnOptions.visible = !SPDSettings.intro();
			btnOptions.setPos(startBtn.right(), startBtn.top());

			optionsPane.setPos(btnOptions.right(), btnOptions.top() - optionsPane.height() - 2);

			align(optionsPane);

		} else {
			infoButton.visible = true;
			infoButton.setPos(startBtn.right(), startBtn.top());

			btnOptions.visible = !SPDSettings.intro(); //TODO is it visible when nothing is usable?
			btnOptions.setPos(startBtn.left()-btnOptions.width(), startBtn.top());

			optionsPane.setPos(heroBtns.get(0).left(), startBtn.top() - optionsPane.height() - 2);
			align(optionsPane);
		}


		updateOptionsColor();
	}

	private float uiAlpha;

	@Override
	public void update() {
		super.update();
		btnExit.visible = btnExit.active = !SPDSettings.intro();
		//do not fade when a window is open
		for (Object v : members){
			if (v instanceof Window) resetFade();
		}
		if (!PixelScene.landscape() && GamesInProgress.selectedClass != null) {
			if (uiAlpha > 0f){
				uiAlpha -= Game.elapsed/4f;
			}
			updateFade();
		}
	}

	private void updateFade(){
		float alpha = GameMath.gate(0f, uiAlpha, 1f);
		title.alpha(alpha);
		for (StyledButton b : heroBtns){
			b.enable(alpha != 0);
			b.alpha(alpha);
		}
		if (heroName != null){
			heroName.alpha(alpha);
			heroDesc.alpha(alpha);
			btnFade.enable(alpha != 0);
			btnFade.icon().alpha(alpha);
		}
		startBtn.enable(alpha != 0);
		startBtn.alpha(alpha);
		btnExit.enable(alpha != 0);
		btnExit.icon().alpha(alpha);
		optionsPane.active = optionsPane.visible && alpha != 0;
		optionsPane.alpha(alpha);
		btnOptions.enable(alpha != 0);
		btnOptions.icon().alpha(alpha);
		infoButton.enable(alpha != 0);
		infoButton.icon().alpha(alpha);

		if (landscape()){

			background.x = (Camera.main.width - background.width())/2f + (40*alpha);

			float fadeLeftScale = 47 * (80 - background.x)/80f;
			fadeLeft.scale.x = 3 + Math.max(fadeLeftScale, 0)*alpha;
			fadeLeft.x = background.x-4;
			fadeRight.x = background.x + background.width() + 4;
		}

		fadeLeft.x = background.x-5;
		fadeRight.x = background.x + background.width() + 5;

		fadeLeft.visible = background.x > 0 || (alpha > 0 && landscape());
		fadeRight.visible = background.x + background.width() < Camera.main.width;
	}

	private void resetFade(){
		//starts fading after 4 seconds, fades over 4 seconds.
		uiAlpha = 2f;
		updateFade();
	}

	@Override
	protected void onBackPressed() {
		if (btnExit.active){
			ShatteredPixelDungeon.switchScene(TitleScene.class);
		} else {
			super.onBackPressed();
		}
	}

	private class HeroBtn extends StyledButton {

		private HeroClass cl;

		private static final int MIN_WIDTH = 20;
		private static final int HEIGHT = 24;

		HeroBtn ( HeroClass cl ){
			super(Chrome.Type.GREY_BUTTON_TR, "");

			this.cl = cl;

			icon(new Image(cl.spritesheet(), 0, 90, 12, 15));

		}

		@Override
		public void update() {
			super.update();
			if (cl != GamesInProgress.selectedClass){
				if (!cl.isUnlocked()){
					icon.brightness(0.1f);
				} else {
					icon.brightness(0.6f);
				}
			} else {
				icon.brightness(1f);
			}
		}

		@Override
		protected void onClick() {
			super.onClick();

			if( !cl.isUnlocked() ){
				ShatteredPixelDungeon.scene().addToFront( new WndMessage(cl.unlockMsg()));
			} else if (GamesInProgress.selectedClass == cl) {
				Window w = new WndHeroInfo(cl);
				if (landscape()){
					w.offset(40, 0);
				}
				ShatteredPixelDungeon.scene().addToFront(w);
			} else {
				setSelectedHero(cl);
			}
		}
	}

	private class GameOptions extends Component {

		private NinePatch bg;

		private ArrayList<StyledButton> buttons;
		private ArrayList<ColorBlock> spacers;

		@Override
		protected void createChildren() {

			bg = Chrome.get(Chrome.Type.GREY_BUTTON_TR);
			add(bg);

			buttons = new ArrayList<>();
			spacers = new ArrayList<>();
			if (DeviceCompat.isDebug() || Badges.isUnlocked(Badges.Badge.VICTORY)){
				StyledButton seedButton = new StyledButton(Chrome.Type.BLANK, Messages.get(HeroSelectScene.class, "custom_seed"), 6){
					@Override
					protected void onClick() {
						String existingSeedtext = SPDSettings.customSeed();
						ShatteredPixelDungeon.scene().addToFront( new WndTextInput(Messages.get(HeroSelectScene.class, "custom_seed_title"),
								Messages.get(HeroSelectScene.class, "custom_seed_desc"),
								existingSeedtext,
								20,
								false,
								Messages.get(HeroSelectScene.class, "custom_seed_set"),
								Messages.get(HeroSelectScene.class, "custom_seed_clear")){
							@Override
							public void onSelect(boolean positive, String text) {
								text = DungeonSeed.formatText(text);
								long seed = DungeonSeed.convertFromText(text);

								if (positive && seed != -1){

									for (GamesInProgress.Info info : GamesInProgress.checkAll()){
										if (info.customSeed.isEmpty() && info.seed == seed){
											SPDSettings.customSeed("");
											icon.resetColor();
											ShatteredPixelDungeon.scene().addToFront(new WndMessage(Messages.get(HeroSelectScene.class, "custom_seed_duplicate")));
											return;
										}
									}

									SPDSettings.customSeed(text);
									icon.hardlight(1f, 1.5f, 0.67f);
								} else {
									SPDSettings.customSeed("");
									icon.resetColor();
								}
								updateOptionsColor();
							}
						});
					}
				};
				seedButton.leftJustify = true;
				seedButton.icon(Icons.get(Icons.SEED));
				if (!SPDSettings.customSeed().isEmpty()) seedButton.icon().hardlight(1f, 1.5f, 0.67f);;
				buttons.add(seedButton);
				add(seedButton);

				StyledButton dailyButton = new StyledButton(Chrome.Type.BLANK, Messages.get(HeroSelectScene.class, "daily"), 6){

					private static final long SECOND = 1000;
					private static final long MINUTE = 60 * SECOND;
					private static final long HOUR = 60 * MINUTE;
					private static final long DAY = 24 * HOUR;

					@Override
					protected void onClick() {
						super.onClick();

						long diff = (SPDSettings.lastDaily() + DAY) - Game.realTime;
						if (diff > 24*HOUR){
							ShatteredPixelDungeon.scene().addToFront(new WndMessage(Messages.get(HeroSelectScene.class, "daily_unavailable_long", (diff / DAY)+1)));
							return;
						}

						for (GamesInProgress.Info game : GamesInProgress.checkAll()){
							if (game.daily){
								ShatteredPixelDungeon.scene().addToFront(new WndMessage(Messages.get(HeroSelectScene.class, "daily_existing")));
								return;
							}
						}

						Image icon = Icons.get(Icons.CALENDAR);
						if (diff <= 0)  icon.hardlight(0.5f, 1f, 2f);
						else            icon.hardlight(1f, 0.5f, 2f);
						ShatteredPixelDungeon.scene().addToFront(new WndOptions(
								icon,
								Messages.get(HeroSelectScene.class, "daily"),
								diff > 0 ?
									Messages.get(HeroSelectScene.class, "daily_repeat") :
									Messages.get(HeroSelectScene.class, "daily_desc"),
								Messages.get(HeroSelectScene.class, "daily_yes"),
								Messages.get(HeroSelectScene.class, "daily_no")){
							@Override
							protected void onSelect(int index) {
								if (index == 0){
									if (diff <= 0) {
										long time = Game.realTime - (Game.realTime % DAY);

										//earliest possible daily for v1.4.0 is Sept 10 2022
										//which is 19,245 days after Jan 1 1970
										time = Math.max(time, 19_245 * DAY);

										SPDSettings.lastDaily(time);
										Dungeon.dailyReplay = false;
									} else {
										Dungeon.dailyReplay = true;
									}

									Dungeon.hero = null;
									Dungeon.daily = true;
									ActionIndicator.action = null;
									InterlevelScene.mode = InterlevelScene.Mode.DESCEND;

									Game.switchScene( InterlevelScene.class );
								}
							}
						});
					}

					private long timeToUpdate = 0;

					private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
					{
						dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
					}

					@Override
					public void update() {
						super.update();

						if (Game.realTime > timeToUpdate && visible){
							long diff = (SPDSettings.lastDaily() + DAY) - Game.realTime;

							if (diff > 0){
								if (diff > 30*HOUR){
									text("30:00:00+");
								} else {
									text(dateFormat.format(new Date(diff)));
								}
								timeToUpdate = Game.realTime + SECOND;
							} else {
								text(Messages.get(HeroSelectScene.class, "daily"));
								timeToUpdate = Long.MAX_VALUE;
							}
						}

					}
				};
				dailyButton.leftJustify = true;
				dailyButton.icon(Icons.get(Icons.CALENDAR));
				add(dailyButton);
				buttons.add(dailyButton);

				StyledButton challengeButton = new StyledButton(Chrome.Type.BLANK, Messages.get(WndChallenges.class, "title"), 6){
					@Override
					protected void onClick() {
						ShatteredPixelDungeon.scene().addToFront(new WndChallenges(SPDSettings.challenges(), true) {
							public void onBackPressed() {
								super.onBackPressed();
								icon(Icons.get(SPDSettings.challenges() > 0 ? Icons.CHALLENGE_ON : Icons.CHALLENGE_OFF));
								updateOptionsColor();
							}
						} );
					}
				};
				challengeButton.leftJustify = true;
				challengeButton.icon(Icons.get(SPDSettings.challenges() > 0 ? Icons.CHALLENGE_ON : Icons.CHALLENGE_OFF));
				add(challengeButton);
				buttons.add(challengeButton);
			}

			for (int i = 1; i < buttons.size(); i++){
				ColorBlock spc = new ColorBlock(1, 1, 0xFF000000);
				add(spc);
				spacers.add(spc);
			}
		}

		@Override
		protected void layout() {
			super.layout();

			bg.x = x;
			bg.y = y;

			int width = 0;
			for (StyledButton btn : buttons){
				if (width < btn.reqWidth()) width = (int)btn.reqWidth();
			}
			width += bg.marginHor();

			int top = (int)y + bg.marginTop() - 1;
			int i = 0;
			for (StyledButton btn : buttons){
				btn.setRect(x+bg.marginLeft(), top, width - bg.marginHor(), 16);
				top = (int)btn.bottom();
				if (i < spacers.size()) {
					spacers.get(i).size(btn.width(), 1);
					spacers.get(i).x = btn.left();
					spacers.get(i).y = PixelScene.align(btn.bottom()-0.5f);
					i++;
				}
			}

			this.width = width;
			this.height = top+bg.marginBottom()-y-1;
			bg.size(this.width, this.height);

		}

		private void alpha( float value ){
			bg.alpha(value);

			for (StyledButton btn : buttons){
				btn.alpha(value);
			}

			for (ColorBlock spc : spacers){
				spc.alpha(value);
			}
		}
	}

}
