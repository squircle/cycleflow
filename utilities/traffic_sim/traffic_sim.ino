#define GREEN_DURATION 6000
#define ALL_RED_DURATION 2000
#define YELLOW_DURATION 2000
#define UPDATE_INTERVAL 1000

#define RED_STATE 'R'
//#define YELLOW_STATE 'Y'
#define GREEN_STATE 'G'

#define BUFFER_SIZE 16

int north_timer = 0;
int south_timer = 0;
int east_timer = 0;
int west_timer = 0;

char north_state = RED_STATE;
char south_state = RED_STATE;
char east_state = RED_STATE;
char west_state = RED_STATE;

char *buff = (char*)malloc(BUFFER_SIZE * sizeof(char));

void ns_update(char state, int offset){
  north_state = state;
  south_state = state;
  
  switch(state){
	case RED_STATE:
		north_timer = GREEN_DURATION+ALL_RED_DURATION+YELLOW_DURATION+offset;
		south_timer = GREEN_DURATION+ALL_RED_DURATION+YELLOW_DURATION+offset;
		break;
		
	// case YELLOW_STATE:
		// north_timer = YELLOW_DURATION;
		// south_timer = YELLOW_DURATION;
		// break;
		
	case GREEN_STATE:
		north_timer = GREEN_DURATION;
		south_timer = GREEN_DURATION;
		break;
	
	default:
		Serial.println("ERROR");
  }
}

void ew_update(char state, int offset){
  east_state = state;
  west_state = state;
  
  switch(state){
	case RED_STATE:
		east_timer = GREEN_DURATION+ALL_RED_DURATION+YELLOW_DURATION+offset;
		west_timer = GREEN_DURATION+ALL_RED_DURATION+YELLOW_DURATION+offset;
		break;
		
	// case YELLOW_STATE:
		// east_timer = YELLOW_DURATION;
		// west_timer = YELLOW_DURATION;
		// break;
		
	case GREEN_STATE:
		east_timer = GREEN_DURATION;
		west_timer = GREEN_DURATION;
		break;
	
	default:
		Serial.println("ERROR");
  }
}

void setup(){                
  Serial.begin(38400);
  Serial.println("Program start!");
  ns_update(GREEN_STATE, 0);
  ew_update(RED_STATE, 0);
  
  Serial1.begin(9600);
}

void loop()
{
	sprintf(buff, "N-%c-%d\n", north_state, north_timer);
	Serial.print(buff);
  Serial1.print(buff);
	
	sprintf(buff, "S-%c-%d\n", south_state, south_timer);
	Serial.print(buff);
  Serial1.print(buff);
	
	sprintf(buff, "E-%c-%d\n", east_state, east_timer);
	Serial.print(buff);
  Serial1.print(buff);
	
	sprintf(buff, "W-%c-%d\n", west_state, west_timer);
	Serial.print(buff);
  Serial1.print(buff);
	
	north_timer -= UPDATE_INTERVAL;
	south_timer -= UPDATE_INTERVAL;
	east_timer -= UPDATE_INTERVAL;
	west_timer -= UPDATE_INTERVAL;
	
	if (north_timer < 0 || south_timer < 0) {
		if (north_state == GREEN_STATE || south_state == GREEN_STATE){
			ns_update(RED_STATE, east_timer);
		}
		else {
			ns_update(GREEN_STATE, 0);
		}
	}
	if (east_timer < 0 || west_timer < 0) {
		if (east_state == GREEN_STATE || west_state == GREEN_STATE){
			ew_update(RED_STATE, north_timer);
		}
		else {
			ew_update(GREEN_STATE, 0);
		}
	}
	delay(UPDATE_INTERVAL);
}
