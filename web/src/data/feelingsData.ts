export interface EmotionDetail {
  color: string;
  sub1: {
    [key: string]: string[];
  };
}

export interface FeelingsHierarchy {
  [coreEmotion: string]: EmotionDetail;
}

export const feelingsData: FeelingsHierarchy = {
  "Sad": {
    "color": "hsl(210, 40%, 65%)",
    "sub1": {
      "Lonely": ["Isolated", "Abandoned"],
      "Vulnerable": ["Victimised", "Fragile"],
      "Despair": ["Grief", "Powerless"],
      "Guilty": ["Ashamed", "Remorseful"],
      "Depressed": ["Empty", "Inferior"],
      "Hurt": ["Disappointed", "Embarrassed"]
    }
  },
  "Disgusted": {
    "color": "hsl(280, 25%, 65%)",
    "sub1": {
      "Repelled": ["Horrified", "Hesitant"],
      "Awful": ["Nauseated", "Detestable"],
      "Disapproving": ["Judgmental", "Embarrassed"],
      "Disappointed": ["Appalled", "Revolted"]
    }
  },
  "Angry": {
    "color": "hsl(5, 45%, 65%)",
    "sub1": {
      "Let down": ["Betrayed", "Resentful"],
      "Humiliated": ["Disrespected", "Ridiculed"],
      "Bitter": ["Indignant", "Violated"],
      "Mad": ["Furious", "Jealous"],
      "Aggressive": ["Provoked", "Hostile"],
      "Frustrated": ["Infuriated", "Annoyed"],
      "Distant": ["Withdrawn", "Numb"],
      "Critical": ["Sceptical", "Dismissive"]
    }
  },
  "Fearful": {
    "color": "hsl(175, 35%, 60%)",
    "sub1": {
      "Scared": ["Helpless", "Frightened"],
      "Anxious": ["Overwhelmed", "Worried"],
      "Insecure": ["Inadequate", "Inferior"],
      "Weak": ["Worthless", "Insignificant"],
      "Rejected": ["Excluded", "Persecuted"],
      "Threatened": ["Nervous", "Exposed"]
    }
  },
  "Bad": {
    "color": "hsl(140, 30%, 60%)",
    "sub1": {
      "Bored": ["Indifferent", "Apathetic"],
      "Busy": ["Pressured", "Rushed"],
      "Stressed": ["Overwhelmed", "Out of control"],
      "Tired": ["Sleepy", "Unfocused"]
    }
  },
  "Surprised": {
    "color": "hsl(35, 55%, 65%)",
    "sub1": {
      "Startled": ["Shocked", "Dismayed"],
      "Confused": ["Disillusioned", "Perplexed"],
      "Amazed": ["Astonished", "Awe"],
      "Excited": ["Eager", "Energetic"]
    }
  },
  "Happy": {
    "color": "hsl(15, 60%, 70%)",
    "sub1": {
      "Playful": ["Cheeky", "Free"],
      "Content": ["Joyful", "Curious"],
      "Interested": ["Inquisitive", "Successful"],
      "Proud": ["Confident", "Respected"],
      "Accepted": ["Valued", "Courageous"],
      "Powerful": ["Creative", "Loving"],
      "Peaceful": ["Thankful", "Sensitive"],
      "Trusting": ["Intimate", "Hopeful"],
      "Optimistic": ["Inspired", "Open"]
    }
  }
};
