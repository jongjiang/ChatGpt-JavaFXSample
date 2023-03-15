'''
這是一份模擬 John Bumpass Calhoun 的 Universe 25 實驗的 Python 代碼。我們添加了一個 social_rank 屬性來決定每個老鼠在人口中的社會地位。社會地位將根據每個老鼠的年齡和後代數量來確定，年齡較大且後代較多的老鼠將具有更高的社會地位。
我們也添加了一個方法來檢查老鼠的社會行為，以及一個方法來實施社會干預以改善人口的福祉。
'''

import random
import datetime

class Mouse:
    def __init__(self, id_num):
        self.id = id_num
        self.is_alive = True
        self.is_pregnant = False
        self.gender = random.choice(['male', 'female'])
        self.age = 0
        self.num_offspring = 0
        self.social_rank = 0
    
    def __repr__(self):
        return f"Mouse {self.id}"
    
    def age_mouse(self):
        self.age += 1
        if self.age >= 50:
            self.is_alive = False
        if self.is_pregnant:
            self.num_offspring += 1
    
    def get_pregnant(self):
        self.is_pregnant = True
    
    def give_birth(self):
        self.is_pregnant = False
        self.num_offspring += 1
        return Mouse(id_num=random.randint(1, 100000))
    
    def update_social_rank(self):
        self.social_rank = self.age + self.num_offspring
    

class Universe25:
    def __init__(self, initial_population=20, max_population=200):
        self.mouse_list = [Mouse(id_num) for id_num in range(1, initial_population+1)]
        self.max_population = max_population
        self.time_step = 0
    
    def run_experiment(self, intervention):
        print(f"len(self.mouse_list)={len(self.mouse_list)}")
        while len(self.mouse_list) < self.max_population and len(self.mouse_list) > 0:
            self.time_step += 1
            births = []
            deaths = []
            for mouse in self.mouse_list:
                mouse.age_mouse()
                mouse.update_social_rank()
                # 隨機懷孕
                if mouse.gender == 'female' and mouse.age >= 2 and intervention != "improve environment":
                    mouse.is_pregnant = random.choice([False, True])
                if mouse.is_pregnant:
                    births.append(mouse.give_birth())
                if not mouse.is_alive:
                    deaths.append(mouse)
            for dead_mouse in deaths:
                self.mouse_list.remove(dead_mouse)
            for new_mouse in births:
                self.mouse_list.append(new_mouse)
                if len(self.mouse_list) >= self.max_population:
                    break
            if len(self.mouse_list) > 0:
                self.check_behavior()
                self.check_social_behavior()
    
    def check_behavior(self):
        num_mice = len(self.mouse_list)
        if num_mice <= 10:
            print(f"時間 {self.time_step}，人口: {num_mice}，沒有異常行為。")
        elif num_mice <= 30:
            print(f"時間 {self.time_step}，人口: {num_mice}，觀察到輕微的攻擊和性行為。")
        elif num_mice <= 100:
            print(f"時間 {self.time_step}，人口: {num_mice}，觀察到明顯的攻擊、性行為和繁殖失調。")
        else:
            print(f"時間 {self.time_step}，人口: {num_mice}，觀察到嚴重的社會混亂和大量死亡。")

    def check_social_behavior(self):
        social_ranks = [mouse.social_rank for mouse in self.mouse_list]
        median_rank = sorted(social_ranks)[len(social_ranks) // 2]
        if median_rank < 10:
            print(f"時間 {self.time_step}，人口: {len(self.mouse_list)}，觀察到低社會凝聚力。")
        elif median_rank < 20:
            print(f"時間 {self.time_step}，人口: {len(self.mouse_list)}，觀察到中等社會凝聚力。")
        else:
            print(f"時間 {self.time_step}，人口: {len(self.mouse_list)}，觀察到高社會凝聚力。")

    def implement_social_intervention(self, intervention):
        random.seed(datetime.datetime.now)
        if intervention == "reduce population":
            self.mouse_list = random.sample(self.mouse_list, k=int(len(self.mouse_list)*0.5))
            print(f"時間 {self.time_step}，人口減少到 {len(self.mouse_list)}。")
        elif intervention == "improve environment":
            for mouse in self.mouse_list:
                mouse.is_pregnant = False
            print(f"時間 {self.time_step}，環境改善，沒有懷孕的老鼠了。")

    def get_population_size(self):
        return len(self.mouse_list)

print("===============================================")
#現在我們可以運行模擬，觀察老鼠的社會行為。我們還可以實施社會干預以改善人口的福祉。例如，我們可以減少人口50％或改善環境以防止老鼠懷孕：
universe = Universe25(initial_population=20, max_population=200)
universe.run_experiment("")

print("=============實施社會干預以減少人口============")
#實施社會干預以減少人口
universe = Universe25(initial_population=20, max_population=200)
universe.implement_social_intervention("reduce population")

print("=============觀察==============================")
#觀察干預後的人口和社會行為
universe.run_experiment("reduce population")

print("=============實施改善環境的社會干預============")
#實施改善環境的社會干預
universe = Universe25(initial_population=20, max_population=200)
universe.implement_social_intervention("improve environment")

print("=============觀察==============================")
#觀察干預後的人口和社會行為
universe.run_experiment("improve environment")