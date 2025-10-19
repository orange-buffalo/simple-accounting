#!/bin/bash
set -e

# Script to periodically collect thread dumps from Gradle test processes
# This helps diagnose hanging tests by capturing the state of threads

DUMP_DIR="${1:-thread-dumps}"
INTERVAL="${2:-30}"  # seconds between dumps
MAX_DUMPS="${3:-40}"  # maximum number of dumps to collect (40 dumps * 30s = 20 min)

mkdir -p "$DUMP_DIR"

echo "Thread dump collection started"
echo "  Dump directory: $DUMP_DIR"
echo "  Interval: ${INTERVAL}s"
echo "  Max dumps: $MAX_DUMPS"

dump_count=0

while [ $dump_count -lt $MAX_DUMPS ]; do
    sleep "$INTERVAL"
    
    # Find all Java processes that are running Gradle tests
    # Look for processes with "GradleWorkerMain" or test-related patterns
    java_pids=$(pgrep -f "GradleWorkerMain|org.gradle.process.internal.worker.GradleWorkerMain" || true)
    
    if [ -z "$java_pids" ]; then
        echo "No Gradle worker processes found, checking for any Gradle Java processes..."
        # Fallback: look for any Gradle-related Java processes
        java_pids=$(pgrep -f "gradle.*java" || true)
    fi
    
    if [ -n "$java_pids" ]; then
        dump_count=$((dump_count + 1))
        timestamp=$(date +%Y%m%d_%H%M%S)
        
        echo "[$timestamp] Collecting thread dump #$dump_count"
        
        for pid in $java_pids; do
            # Check if process still exists
            if kill -0 "$pid" 2>/dev/null; then
                dump_file="$DUMP_DIR/threaddump_${timestamp}_pid${pid}.txt"
                
                # Try jstack first (more detailed), fall back to jcmd, then kill -3
                if command -v jstack &> /dev/null; then
                    echo "  Using jstack for PID $pid"
                    jstack -l "$pid" > "$dump_file" 2>&1 || echo "jstack failed for PID $pid" >> "$dump_file"
                elif command -v jcmd &> /dev/null; then
                    echo "  Using jcmd for PID $pid"
                    jcmd "$pid" Thread.print > "$dump_file" 2>&1 || echo "jcmd failed for PID $pid" >> "$dump_file"
                else
                    echo "  Using kill -3 for PID $pid"
                    # This sends SIGQUIT which triggers thread dump to stderr/stdout
                    kill -3 "$pid" 2>&1 | tee "$dump_file"
                fi
                
                # Also capture process info
                echo -e "\n\n=== Process Info ===" >> "$dump_file"
                ps -f -p "$pid" >> "$dump_file" 2>&1 || true
                
                # Capture open files/sockets
                echo -e "\n\n=== Open Files (sample) ===" >> "$dump_file"
                lsof -p "$pid" 2>&1 | head -50 >> "$dump_file" || echo "lsof not available or failed" >> "$dump_file"
                
                # Capture thread count
                echo -e "\n\n=== Thread Count ===" >> "$dump_file"
                ps -o nlwp -p "$pid" >> "$dump_file" 2>&1 || true
                
                echo "  Thread dump saved to $dump_file"
            fi
        done
    else
        echo "[$timestamp] No Java processes found (dump attempt #$dump_count)"
    fi
done

echo "Thread dump collection completed ($dump_count dumps collected)"
