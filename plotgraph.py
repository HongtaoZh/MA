from matplotlib import pyplot as plt
from matplotlib import style
import numpy as np

style.use('ggplot')


for sim in ['query0-bandwidth-everymin.csv', 'query0-bandwidth-everysec.csv', 'query0-latency-everymin.csv',
            'query0-latency-everysec.csv', 'query0-latencybandwidth-everymin.csv',
            'query0-latencybandwidth-everysec.csv']:
    times,latencystaticms,latencyadaptivems,bandwidthstatic,bandwidthadaptive = np.loadtxt(
                     sim,
                     unpack=True,
                     delimiter = ',')

    plt.plot(times,latencystaticms,label='Static Latency')
    plt.plot(times,latencyadaptivems,label='Adaptive Latency')

    plt.title('Preliminary Evaluation')
    plt.ylabel('Time'+sim.split("-")[2])
    plt.xlabel(sim.split("-")[1])
    plt.savefig(sim.split(".")[0]+".png")
    plt.clf()
